package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.item.vo.SkuVo;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecifictionClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecifictionClient specClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsRepository goodsRepository;

    /**
     * 构建Goods对象
     * @param spu
     * @return
     */
    public Goods buildGoods(Spu spu){

        Long spuId = spu.getId();

        // 获取all字段的拼接
        String all = this.getall(spu);
        // 需要对sku过滤把不需要的数据去掉
        List<Sku> skuList = goodsClient.querySkuBySpuId(spuId);
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        List<SkuVo> skuVoList = this.getSkuVo(skuList);
        Set<Long> priceList = skuList.stream().map(Sku::getPrice).collect(Collectors.toSet());

        //查询规格参数
        List<SpecParam> params = specClient.queryParamByList(null, spu.getCid3(), true);
        if(CollectionUtils.isEmpty(params)){
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.queryDetailById(spuId);
        // 获取通用规格参数
        Map<Long, String> genericSpec = JsonUtils.toMap(
                spuDetail.getGenericSpec(), Long.class, String.class);

        //获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(
                spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
                });
        //规格参数
        Map<String,Object> specs = new HashMap<>();

        //对规格进行遍历，并封装spec，其中spec的key是规格参数的名称，value是商品详情中的值
        for (SpecParam param : params) {
            //key是规格参数的名称
            String key = param.getName();
            Object value = "";

            if (param.getGeneric()) {
                //参数是通用属性，通过规格参数的ID从商品详情存储的规格参数中查出值
                value = genericSpec.get(param.getId());
                if (param.getNumeric()) {
                    //参数是数值类型，处理成段，方便后期对数值类型进行范围过滤
                    value = chooseSegment(value.toString(), param);
                }
            } else {
                //参数不是通用类型
                value = specialSpec.get(param.getId());
            }
            value = (value == null ? "其他" : value);
            //存入map
            specs.put(key, value);
        }

        //构建goods对象
        Goods goods = new Goods();
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spu.getId());
        goods.setAll(all);
        goods.setPrice(priceList); // 所有sku的价格集合
        goods.setSkus(JsonUtils.toString(skuVoList)); // 所有sku集合的json格式
        goods.setSpecs(specs);  // 所有可搜索的规格参数
        goods.setSubTitle(spu.getSubTitle());
        return goods;
    }

    /**
     * 对all字段进行拼接
     *
     * @param spu
     * @return
     */
    private String getall(Spu spu) {
        // 查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categories)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FIND);
        }
        // 查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //品牌名
        List<String> names = categories.stream().map(Category::getName).collect(Collectors.toList());
        // 搜索字段
        String all = spu.getTitle() + StringUtils.join(names, ",") + brand.getName();
        return all;
    }

    /**
     * 对sku进行处理，去掉不需要的字段
     *
     * @param skus
     * @return
     */
    private List<SkuVo> getSkuVo(List<Sku> skus) {
        List<SkuVo> skuVoList = new ArrayList<>();
        for (Sku sku : skus) {
            SkuVo skuVo = new SkuVo();
            skuVo.setId(sku.getId());
            skuVo.setPrice(sku.getPrice());
            skuVo.setTitle(sku.getTitle());
            skuVo.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            skuVoList.add(skuVo);
        }
        return skuVoList;
    }

    /**
     * 对数值进行分段
     *
     * @param value
     * @param p
     * @return
     */
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest request) {
        int page = request.getPage() - 1;
        int size = request.getSize();
        // 查询构建器，从elasticsearch获取数据
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","subTitle","skus"},null));
        // 分页
        queryBuilder.withPageable(PageRequest.of(page,size));
        // 过滤
        queryBuilder.withQuery(QueryBuilders.matchQuery("all",request.getKey()));
        // 查询
        Page<Goods> result = goodsRepository.search(queryBuilder.build());
        // 解析结果
        long total = result.getTotalElements();
        int totalPage = result.getTotalPages();
        List<Goods> goodsList = result.getContent();

        return new PageResult<>(total ,totalPage, goodsList);
    }
}

package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.mapper.BrandMapper;
import com.baidu.shop.mapper.CategoryBrandMapper;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.PinyinUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName BrandServiceImpl
 * @Description: TODO
 * @Author zhangxiangxing
 * @Date 2020/8/31
 * @Version V1.0
 **/
@RestController
public class BrandServiceImpl extends BaseApiService implements BrandService {

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private CategoryBrandMapper categoryBrandMapper;

    //通过分类id查询品牌信息
    @Override
    public Result<JsonObject> getBrandByCate(Integer cid) {
        if(ObjectUtil.isNotNull(cid)) {
            List<BrandEntity> list = brandMapper.getBrandByCateId(cid);
            return this.setResultSuccess(list);
        }
        return null;
    }

    //查询品牌信息
    @Override
    public Result<PageInfo<BrandEntity>> getBrandInfo(BrandDTO brandDTO) {

        //分页-->(当前页,每页显示的条数)
        if(ObjectUtil.isNotNull(brandDTO.getPage()) && ObjectUtil.isNotNull(brandDTO.getRows()))
        PageHelper.startPage(brandDTO.getPage(),brandDTO.getRows());


        Example example = new Example(BrandEntity.class);
        //查询条件-->排序字段
        if (StringUtil.isNotEmpty(brandDTO.getSort()))
            example.setOrderByClause(brandDTO.getOrderByClause());
        //查询条件-->品牌名称
        if (StringUtil.isNotEmpty(brandDTO.getName()))
            example.createCriteria().andLike("name","%" + brandDTO.getName() + "%");

        List<BrandEntity> list = brandMapper.selectByExample(example);

        //数据封装
        PageInfo<BrandEntity> pageInfo = new PageInfo<>(list);

        //返回
        return this.setResultSuccess(pageInfo);
    }

    //新增品牌信息并且可以返回主键
    @Transactional
    @Override
    public Result<JsonObject> saveBrand(BrandDTO brandDTO){

        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO,BrandEntity.class);

        //获取拼音的首字母
        //统一转为大写
//优化前--->优化后
//String name = brandEntity.getName();//获取到品牌名称
//char c = name.charAt(0);//获取到品牌的第一个字符
//String upperCase = PinyinUtil.getUpperCase(String.valueOf(c), PinyinUtil.TO_FIRST_CHAR_PINYIN);//将第一个字符转换拼音
//brandEntity.setLetter(upperCase.charAt(0));//将首字母添加到实体类中
        brandEntity.setLetter(PinyinUtil.getUpperCase(String.valueOf(brandEntity.getName().charAt(0))
                             ,PinyinUtil.TO_FIRST_CHAR_PINYIN).charAt(0));

        brandMapper.insertSelective(brandEntity);

        this.insertCategoryAndBrand(brandDTO, brandEntity);

        //返回
        return this.setResultSuccess();

            //分割 得到数组,批量新增
//String[] cidArr = brandDTO.getCategory().split(",");//通过split方法分割字符串的Array
//List<String> list = Arrays.asList(cidArr);//Array.asList将Array转换为List
//List<CategoryBrandEntity> categoryBrandEntities = new ArrayList<>();
//list.stream().forEach(cid -> {//使用JDK1.8的stream,使用map函数返回一个新的数据
//  CategoryBrandEntity categoryBrandEntity = new CategoryBrandEntity();
//  categoryBrandEntity.setCategoryId(StringUtil.toInteger(cid));
//  categoryBrandEntity.setBrandId(brandEntity.getId());
//
//  categoryBrandEntities.add(categoryBrandEntity);
//});

        //形式一:这是最简单的新增操作
        //BaiduBeanUtil.copyProperties(T1,T2)==>将T1(DTO)类型转换成T2(Entry)
        //brandMapper.insertSelective(BaiduBeanUtil.copyProperties(brandDTO,BrandEntity.class));
    }

    //修改品牌信息
    @Transactional
    @Override
    public Result<JsonObject> editBrand(BrandDTO brandDTO) {

        BrandEntity brandEntity = BaiduBeanUtil.copyProperties(brandDTO, BrandEntity.class);
        brandEntity.setLetter(PinyinUtil.getUpperCase(String.valueOf(brandEntity.getName().charAt(0))
                , PinyinUtil.TO_FIRST_CHAR_PINYIN).charAt(0));

        //执行修改操作
        brandMapper.updateByPrimaryKey(brandEntity);

        //通过brandId删除中间表的数据
//        Example example = new Example(CategoryBrandEntity.class);
//        example.createCriteria().andEqualTo("brandId",brandEntity.getId());
//        categoryBrandMapper.deleteByExample(example);

        //通过brandID删除中间表的数据
        this.deleteCategoryAndBrand(brandEntity.getId());

        //新增新的数据
        this.insertCategoryAndBrand(brandDTO,brandEntity);

        return this.setResultSuccess();
    }

    //通过id删除品牌信息
    @Transactional
    @Override
    public Result<JsonObject> deleteBrand(Integer id) {

        //删除品牌
        brandMapper.deleteByPrimaryKey(id);
        //关系
        this.deleteCategoryAndBrand(id);

        return this.setResultSuccess();
    }


    //----------------------封装------------------------------
    private void deleteCategoryAndBrand(Integer id){

        Example example = new Example(CategoryBrandEntity.class);
        example.createCriteria().andEqualTo("brandId",id);
        categoryBrandMapper.deleteByExample(example);
    }

    private void insertCategoryAndBrand(BrandDTO brandDTO, BrandEntity brandEntity){

        if(brandDTO.getCategory().contains(",")){
            List<CategoryBrandEntity> categoryBrandEntities = Arrays.asList(brandDTO.getCategory().split(","))
                    .stream().map(cid -> {
                        CategoryBrandEntity entity = new CategoryBrandEntity();
                        entity.setCategoryId(StringUtil.toInteger(cid));
                        entity.setBrandId(brandEntity.getId());

                        return entity;
                    }).collect(Collectors.toList());

            categoryBrandMapper.insertList(categoryBrandEntities);
        }else {

            CategoryBrandEntity entity = new CategoryBrandEntity();
            entity.setCategoryId(StringUtil.toInteger(brandDTO.getCategory()));
            entity.setBrandId(brandEntity.getId());

            categoryBrandMapper.insertSelective(entity);
        }

    }
}

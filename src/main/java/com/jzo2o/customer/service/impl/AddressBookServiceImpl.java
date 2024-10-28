package com.jzo2o.customer.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.api.publics.MapApi;
import com.jzo2o.api.publics.dto.response.LocationResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.BeanUtils;
import com.jzo2o.common.utils.CollUtils;
import com.jzo2o.common.utils.NumberUtils;
import com.jzo2o.common.utils.StringUtils;
import com.jzo2o.customer.mapper.AddressBookMapper;
import com.jzo2o.customer.model.domain.AddressBook;
import com.jzo2o.customer.model.dto.request.AddressBookPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.AddressBookUpsertReqDTO;
import com.jzo2o.customer.service.IAddressBookService;
import com.jzo2o.mvc.utils.UserContext;
import com.jzo2o.mysql.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 地址薄 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-07-06
 */
@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements IAddressBookService {

    @Resource
    private MapApi mapApi;

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 根据用户id和城市编码查询地址簿列表
     * @param userId 用户id
     * @param city 城市编码
     * @return List<AddressBookResDTO>
     */
    @Override
    public List<AddressBookResDTO> getByUserIdAndCity(Long userId, String city) {

        List<AddressBook> addressBooks = lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getCity, city)
                .list();
        if(CollUtils.isEmpty(addressBooks)) {
            return new ArrayList<>();
        }
        return BeanUtils.copyToList(addressBooks, AddressBookResDTO.class);
    }

    /**
     * 新增地址簿
     * @param addressBookUpsertReqDTO 地址簿新增参数
     */
    @Override
    public void add(AddressBookUpsertReqDTO addressBookUpsertReqDTO) {
        // 当前用户id
        Long userId = UserContext.currentUserId();
        // 如果新增地址设为默认，取消其他默认地址
        if (addressBookUpsertReqDTO.getIsDefault() == 1) {
            cancelDefault(userId);
        }
        AddressBook addressBook = BeanUtils.toBean(addressBookUpsertReqDTO, AddressBook.class);
        addressBook.setUserId(userId);
        // 组装详细地址
        String completeAddress = addressBookUpsertReqDTO.getProvince() + addressBookUpsertReqDTO.getCity()
                + addressBookUpsertReqDTO.getCounty() + addressBookUpsertReqDTO.getAddress();
        // 如果详细地址为空，调用高德地图api获取经纬度
        if (ObjectUtil.isEmpty(addressBookUpsertReqDTO.getLocation())) {
            LocationResDTO locationResDTO = mapApi.getLocationByAddress(completeAddress);
            String location = locationResDTO.getLocation();
            addressBookUpsertReqDTO.setLocation(location);
        }
        if (StringUtils.isNotEmpty(addressBookUpsertReqDTO.getLocation())) {
            // 经纬度
            addressBook.setLon(NumberUtils.parseDouble(addressBookUpsertReqDTO.getLocation().split(",")[0]));
            addressBook.setLon(NumberUtils.parseDouble(addressBookUpsertReqDTO.getLocation().split(",")[1]));
        }
        addressBookMapper.insert(addressBook);
    }

    /**
     * 取消默认地址
     * @param userId 用户id
     */
    private void cancelDefault(Long userId) {
        LambdaUpdateWrapper<AddressBook> updateWrapper = Wrappers.<AddressBook>lambdaUpdate()
                .eq(AddressBook::getUserId, userId)
                .set(AddressBook::getIsDefault, 0);
        super.update(updateWrapper);
    }

    /**
     * 分页查询地址簿列表
     * @param addressBookPageQueryReqDTO 地址簿分页查询参数
     * @return PageResult<AddressBookResDTO> 分页结果
     */
    @Override
    public PageResult<AddressBookResDTO> page(AddressBookPageQueryReqDTO addressBookPageQueryReqDTO) {
        Page<AddressBook> page = PageUtils.parsePageQuery(addressBookPageQueryReqDTO, AddressBook.class);
        LambdaQueryWrapper<AddressBook> queryWrapper = Wrappers.<AddressBook>lambdaQuery().eq(AddressBook::getUserId, UserContext.currentUserId());
        Page<AddressBook> addressBookPage = addressBookMapper.selectPage(page, queryWrapper);
        return PageUtils.toPage(addressBookPage, AddressBookResDTO.class);
    }

    /**
     * 修改地址簿
     * @param id 地址簿id
     * @param addressBookUpsertReqDTO 地址簿修改参数
     */
    @Transactional
    @Override
    public void update(Long id, AddressBookUpsertReqDTO addressBookUpsertReqDTO) {
        if (addressBookUpsertReqDTO.getIsDefault() == 1) {
            cancelDefault(UserContext.currentUserId());
        }
        AddressBook addressBook = BeanUtils.toBean(addressBookUpsertReqDTO, AddressBook.class);
        addressBook.setId(id);
        // 调用第三方地图api获取经纬度
        String completeAddress = addressBookUpsertReqDTO.getProvince() + addressBookUpsertReqDTO.getCity()
                + addressBookUpsertReqDTO.getCounty() + addressBookUpsertReqDTO.getAddress();
        LocationResDTO locationResDTO = mapApi.getLocationByAddress(completeAddress);
        String location = locationResDTO.getLocation();
        if (StringUtils.isNotEmpty(location)) {
            addressBook.setLon(NumberUtils.parseDouble(location.split(",")[0]));
            addressBook.setLat(NumberUtils.parseDouble(location.split(",")[1]));
        }
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 修改地址簿默认状态
     * @param userId 用户id
     * @param id 地址簿id
     * @param flag 默认状态
     */
    @Override
    public void updateDefaultStatus(Long userId, Long id, Integer flag) {
        if (flag == 1) {
            cancelDefault(userId);
        }
        AddressBook addressBook = new AddressBook();
        addressBook.setId(id);
        addressBook.setIsDefault(flag);
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 获取默认地址
     * @return AddressBookResDTO
     */
    @Override
    public AddressBookResDTO defaultAddress() {
        LambdaQueryWrapper<AddressBook> queryWrapper = Wrappers.<AddressBook>lambdaQuery()
                .eq(AddressBook::getUserId, UserContext.currentUserId())
                .eq(AddressBook::getIsDefault, 1);
        AddressBook addressBook = addressBookMapper.selectOne(queryWrapper);
        return BeanUtils.toBean(addressBook, AddressBookResDTO.class);
    }
    

}

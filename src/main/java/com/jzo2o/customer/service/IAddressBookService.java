package com.jzo2o.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.domain.AddressBook;
import com.jzo2o.customer.model.dto.request.AddressBookPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.AddressBookUpsertReqDTO;

import java.util.List;

/**
 * <p>
 * 地址薄 服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-06
 */
public interface IAddressBookService extends IService<AddressBook> {

    /**
     * 根据用户id和城市编码获取地址
     * @param userId 用户id
     * @param cityCode 城市编码
     * @return 地址编码
     */
    List<AddressBookResDTO> getByUserIdAndCity(Long userId, String cityCode);

    /**
     * 地址簿新增/更新
     * @param addressBookUpsertReqDTO 新增/更新地址簿参数
     */
    void add(AddressBookUpsertReqDTO addressBookUpsertReqDTO);

    /**
     *  地址簿分页查询
     * @param addressBookPageQueryReqDTO 查询条件
     * @return 分页结果
     */
    PageResult<AddressBookResDTO> page(AddressBookPageQueryReqDTO addressBookPageQueryReqDTO);

    /**
     * 地址簿修改
     * @param id 地址簿id
     * @param addressBookUpsertReqDTO 修改参数
     */
    void update(Long id, AddressBookUpsertReqDTO addressBookUpsertReqDTO);

    /**
     * 设置默认地址
     * @param userId 用户id
     * @param id 地址簿id
     * @param flag 0：取消默认，1：设为默认
     */
    void updateDefaultStatus(Long userId, Long id, Integer flag);

    /**
     * 获取默认地址
     * @return 地址簿信息
     */
    AddressBookResDTO defaultAddress();

}

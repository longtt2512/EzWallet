package com.ezwallet.module.bill.mapper;

import com.ezwallet.module.bill.entity.Bill;
import com.ezwallet.module.bill.dto.BillResponse;
import com.ezwallet.module.bill.dto.BillProviderResponse;
import com.ezwallet.module.bill.entity.BillProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillMapper {

    @Mapping(target = "providerName", source = "provider.name")
    @Mapping(target = "providerCode", source = "provider.code")
    BillResponse toDto(Bill bill);

    BillProviderResponse toDto(BillProvider provider);
}

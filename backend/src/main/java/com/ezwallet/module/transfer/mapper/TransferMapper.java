package com.ezwallet.module.transfer.mapper;

import com.ezwallet.module.transaction.entity.Transaction;
import com.ezwallet.module.transfer.dto.TransferResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "id", source = "id")
    TransferResponse toDto(Transaction tx);
}

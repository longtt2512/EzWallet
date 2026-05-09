package com.ezwallet.module.bill.mapper;

import com.ezwallet.module.bill.dto.BillProviderResponse;
import com.ezwallet.module.bill.dto.BillResponse;
import com.ezwallet.module.bill.entity.Bill;
import com.ezwallet.module.bill.entity.BillProvider;
import com.ezwallet.module.bill.entity.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-09T23:48:30+0700",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.7.jar, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class BillMapperImpl implements BillMapper {

    @Override
    public BillResponse toDto(Bill bill) {
        if ( bill == null ) {
            return null;
        }

        String providerName = null;
        String providerCode = null;
        Long id = null;
        String billCode = null;
        String customerCode = null;
        BigDecimal amount = null;
        BillStatus status = null;
        LocalDate dueDate = null;

        providerName = billProviderName( bill );
        providerCode = billProviderCode( bill );
        id = bill.getId();
        billCode = bill.getBillCode();
        customerCode = bill.getCustomerCode();
        amount = bill.getAmount();
        status = bill.getStatus();
        dueDate = bill.getDueDate();

        BillResponse billResponse = new BillResponse( id, billCode, providerName, providerCode, customerCode, amount, status, dueDate );

        return billResponse;
    }

    @Override
    public BillProviderResponse toDto(BillProvider provider) {
        if ( provider == null ) {
            return null;
        }

        Long id = null;
        String code = null;
        String name = null;
        String serviceType = null;

        id = provider.getId();
        code = provider.getCode();
        name = provider.getName();
        serviceType = provider.getServiceType();

        BillProviderResponse billProviderResponse = new BillProviderResponse( id, code, name, serviceType );

        return billProviderResponse;
    }

    private String billProviderName(Bill bill) {
        if ( bill == null ) {
            return null;
        }
        BillProvider provider = bill.getProvider();
        if ( provider == null ) {
            return null;
        }
        String name = provider.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String billProviderCode(Bill bill) {
        if ( bill == null ) {
            return null;
        }
        BillProvider provider = bill.getProvider();
        if ( provider == null ) {
            return null;
        }
        String code = provider.getCode();
        if ( code == null ) {
            return null;
        }
        return code;
    }
}

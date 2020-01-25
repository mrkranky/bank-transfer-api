package beans.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.Account.CurrencyEnum;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestBean implements Serializable {
    private static final long serialVersionUID = -4625891081259787539L;

    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private CurrencyEnum currency;
}

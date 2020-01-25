package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name="transfer_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Builder
public class TransferLog extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name="from_account_id")
    private Long fromAccountId;

    @Column(name="to_account_id")
    private Long toAccountId;

    @Column
    private BigDecimal amount;

    @Column(name="requested_currency")
    private String requestedCurrency;

    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, COMPLETED, FAILED;

        @Override
        public String toString() {
            return this.name();
        }
    }
}

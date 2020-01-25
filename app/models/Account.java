package models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"customer", "lock", "deletedAt"})
@EqualsAndHashCode(exclude = {"customer", "lock"}, callSuper = false)
@Builder
public class Account extends BaseModel {

    @Transient
    @JsonIgnore
    private Lock lock = new ReentrantLock();

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account-gen")
    @SequenceGenerator(name = "account-gen", sequenceName = "account_seq", initialValue = 19283746, allocationSize = 30)
    private Long id;

    @Column
    private BigDecimal balance;

    @Column
    @Enumerated(EnumType.STRING)
    private CurrencyEnum currency;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_id", insertable = false, updatable = false)
    private Long customerId;

    @JsonIgnore
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name="deleted_at")
    private Date deletedAt;

    public enum CurrencyEnum {
        USD, EUR, SGD;

        @Override
        public String toString() {
            return this.name();
        }
    }
}

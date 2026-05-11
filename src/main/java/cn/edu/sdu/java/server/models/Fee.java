package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 * Fee 消费流水实体类  保存学生消费流水的基本信息信息，
 * Integer feeId 消费表 fee 主键 fee_id
 * Integer personId   对应student 表里面的 person_id
 * String day 日期
 * Double money 金额
 * String consumptionType 消费类型(dining/study/transport/life/entertainment)
 */
@Getter
@Setter
@Entity
@Table(	name = "fee"
)
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer feeId;
    
    @ManyToOne
    @JoinColumn(name = "personId")
    private Student student;
    
    @Size(max = 20)
    private String day;
    
    private Double money;
    
    @Column(name = "consumption_type", length = 20)
    private String consumptionType;  // dining-餐饮, study-学习用品, transport-交通费, life-生活用品, entertainment-娱乐
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}

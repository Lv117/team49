package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "social_relation")
@Getter
@Setter
public class SocialRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer relationId;

    @ManyToOne
    @JoinColumn(name = "personId")
    private Student student;

    @Size(max = 30)
    @Column(name = "relation_type", length = 30)
    private String relationType;

    @Size(max = 30)
    private String name;

    @Size(max = 10)
    private String gender;

    @Size(max = 20)
    private String phone;

    private Integer age;

    @Size(max = 200)
    private String remark;
}

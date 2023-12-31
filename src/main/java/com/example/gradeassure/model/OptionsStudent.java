package com.example.gradeassure.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class OptionsStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String variant;
    private Boolean correct;

    @OneToOne
    @JoinColumn(name = "questionStudent_id")
    private QuestionStudent student;
}

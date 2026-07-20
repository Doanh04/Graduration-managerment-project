package com.graduration.entity;

import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "email")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailDocument {
    @Id
    String idEmailLog;
    String userId;
    String email;
    String subject;
    String content;
    String status;
    Date sendtime;
}

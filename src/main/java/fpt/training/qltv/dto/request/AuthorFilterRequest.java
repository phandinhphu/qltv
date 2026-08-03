package fpt.training.qltv.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorFilterRequest {

    private String name;
    private Boolean deleted;
}

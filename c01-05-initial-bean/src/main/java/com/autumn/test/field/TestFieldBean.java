package com.autumn.test.field;

import com.autumn.annotation.Autowired;
import com.autumn.annotation.Component;
import com.autumn.test.nested.OuterBean;
import com.autumn.test.primary.PersonBean;
import com.autumn.test.primary.StudentBean;
import lombok.Data;
import lombok.ToString;

/**
 * @author huangcanjie
 */
@Component
@Data
public class TestFieldBean {

    @Autowired
    private OuterBean outerBean;

    private PersonBean personBean;

    @Autowired
    public void setPersonBean(StudentBean studentBean) {
        this.personBean = studentBean;
    }

}

package org.proto;

import org.proto.service.UserService;
import org.proto.spring.ApplicationContext;
import org.proto.spring.annotation.ComponentScan;

/**
 * @author  proto
 */
@ComponentScan("org.proto.service")
public class AppContext {



    public static void main(String[] args) {

        ApplicationContext applicationContext = new ApplicationContext(AppContext.class);

        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.exec();
    }



}

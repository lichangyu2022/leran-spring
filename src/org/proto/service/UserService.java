package org.proto.service;

import org.proto.spring.annotation.Autowired;
import org.proto.spring.annotation.Component;

@Component
public class UserService {

    @Autowired
    private RoleService roleService;


    public void exec(){
        roleService.exec();
        System.out.println("user");
    }


}

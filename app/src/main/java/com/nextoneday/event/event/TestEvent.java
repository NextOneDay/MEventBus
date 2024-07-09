package com.nextoneday.event.event;
/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2018-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date	                 Author	                Action
 *  	                   	Create/Add/Modify/Delete
 * ===========================================================================================
 */

/**
 * Author: shah
 * Date : 2020/5/31.
 * Desc : TestEvent
 */
public class TestEvent {

    public String name;
    public int num;

    public TestEvent(String name, int num) {
        this.name = name;
        this.num = num;
    }

    @Override
    public String toString() {
        return "TestEvent{" +
                "name='" + name + '\'' +
                ", num=" + num +
                '}';
    }
}

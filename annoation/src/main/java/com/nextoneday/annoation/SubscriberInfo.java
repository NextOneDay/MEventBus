package com.nextoneday.annoation;
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
 * Date : 2020/5/27.
 * Desc : SubscriberInfo
 */
public interface SubscriberInfo {

    Class<?> getSubscriberClass();
    SubscriberMethod [] getSubscriberMethods();
}

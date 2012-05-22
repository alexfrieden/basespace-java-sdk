/**
* Copyright 2012 Illumina
* 
 * Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*    http://www.apache.org/licenses/LICENSE-2.0
* 
 *  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package com.illumina.basespace.test;

import com.illumina.basespace.BaseSpaceConfiguration;

public abstract class TestBaseSpaceConfiguration implements BaseSpaceConfiguration
{

    @Override
    public String getVersion()
    {
        return "v1pre1";
    }

    @Override
    public String getApiRootUri()
    {
        return "https://api.cloud-endor.illumina.com";
    }

    @Override
    public String getAuthTokenUriFragment()
    {
        return "/oauthv2/token";
    }

    @Override
    public String getAuthorizationUri()
    {
        return "https://cloud-endor.illumina.com/oauth/authorize";
    }

    @Override
    public int getAuthCodeListenerPort()
    {
        return 7777;
    }

}
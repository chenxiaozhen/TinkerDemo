package com.cxz.tinker.info;

import com.cxz.tinker.utils.AppUtils;
import com.cxz.tinker.utils.ApplicationContext;

/**
 * we add BaseBuildInfo to loader pattern, so it won't change with patch!
 */
public class BaseBuildInfo {
    public static String TEST_MESSAGE = "I won't change with tinker patch!";
    public static String BASE_TINKER_ID = AppUtils.getTinkerIdValue(ApplicationContext.application);
}

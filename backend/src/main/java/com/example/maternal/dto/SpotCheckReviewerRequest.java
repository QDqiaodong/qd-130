
package com.example.maternal.dto;

import lombok.Data;

/**
 * 补填当班复核人请求：不合格抽检必须补填复核人后才能转报修。
 */
@Data
public class SpotCheckReviewerRequest {

    /** 当班复核人姓名 */
    private String reviewer;
}

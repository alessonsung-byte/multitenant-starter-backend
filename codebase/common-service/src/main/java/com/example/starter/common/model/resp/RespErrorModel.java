package com.example.starter.common.model.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <code>RespErrorModel</code> 所有 API 錯誤回應的統一格式：錯誤碼＋可以直接顯示給使用者的訊息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespErrorModel {

    private RespMessageCode code;

    private String message;
}

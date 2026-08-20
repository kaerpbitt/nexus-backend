package com.financial.engine.controller;

import com.financial.engine.dto.OrderRequest;
import com.financial.engine.dto.TradeResponse;
import com.financial.engine.service.TradeExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class TradeController {

    private final TradeExecutionService tradeExecutionService;

    public TradeController(TradeExecutionService tradeExecutionService) {
        this.tradeExecutionService = tradeExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<TradeResponse> executeOrder(
            @RequestBody OrderRequest request,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        TradeResponse response = tradeExecutionService.processOrderExecution(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }
}

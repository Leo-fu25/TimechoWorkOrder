package com.timecho.workorder.service;

import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.repository.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SlaMonitorService {
    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderService workOrderService;

    @Scheduled(fixedDelayString = "${sla.monitor.delay-ms:60000}")
    public void monitorAndEscalate() {
        for (WorkOrder workOrder : workOrderRepository.findAll()) {
            workOrderService.checkAndEscalateSla(workOrder);
        }
    }
}

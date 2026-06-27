package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrpcClientService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void sendAction(String hubId, String scenarioName, Action action) {
        try {
            DeviceActionProto deviceActionProto = DeviceActionProto.newBuilder()
                    .setType(ActionTypeProto.SET_VALUE)
                    .setValue(action.getValue())
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(deviceActionProto)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .build())
                    .build();

            hubRouterClient.handleDeviceAction(request);
            log.info("Sent action to hub: hubId={}, scenario={}, type={}, value={}",
                    hubId, scenarioName, action.getType(), action.getValue());
        } catch (Exception e) {
            log.error("Failed to send action to hub: hubId={}, scenario={}", hubId, scenarioName, e);
        }
    }
}
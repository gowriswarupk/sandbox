package com.redhat.service.smartevents.manager;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.utils.TestUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.DEPROVISION;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus.READY;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PAGE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_PAGE_SIZE;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.TestConstants.SHARD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
@QuarkusTestResource(PostgresResource.class)
public class BridgesServiceTest {

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    BridgesService bridgesService;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    RhoasService rhoasServiceMock;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<Bridge> bridges = bridgesService.findByShardIdWithReadyDependencies(SHARD_ID);
        assertThat(bridges.size()).isZero();
    }

    @Test
    public void testGetEmptyBridges() {
        ListResult<Bridge> bridges = bridgesService.getBridges(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getPage()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getSize()).isZero();
    }

    @Test
    public void testGetBridges() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        ListResult<Bridge> bridges = bridgesService.getBridges(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
        assertThat(bridges.getTotal()).isEqualTo(1);
        assertThat(bridges.getPage()).isZero();

        // filter by customer id not implemented yet
        bridges = bridgesService.getBridges("not-the-id", new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isZero();
        assertThat(bridges.getTotal()).isZero();
        assertThat(bridges.getPage()).isZero();
    }

    @Test
    public void testGetBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        Bridge bridge = bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        TestUtils.waitForBridgeToBeReady(bridgesService);

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge).isNotNull();
        assertThat(retrievedBridge.getName()).isEqualTo(bridge.getName());
        assertThat(retrievedBridge.getCustomerId()).isEqualTo(bridge.getCustomerId());
        // Bridges are moved to the PREPARING status by Workers
        assertThat(retrievedBridge.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);
        assertThat(retrievedBridge.getShardId()).isEqualTo(SHARD_ID);
    }

    @Test
    public void testGetUnexistingBridge() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("not-the-id", DEFAULT_CUSTOMER_ID));
    }

    @Test
    public void testGetBridgeWithWrongCustomerId() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge(bridge.getId(), "not-the-customerId"));
    }

    @Test
    public void testCreateBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);
        assertThat(bridge.getEndpoint()).isNull();

        ListResult<Bridge> bridges = bridgesService.getBridges(DEFAULT_CUSTOMER_ID, new QueryResourceInfo(DEFAULT_PAGE, DEFAULT_PAGE_SIZE));
        assertThat(bridges.getSize()).isEqualTo(1);
        assertThat(bridges.getItems().get(0).getOrganisationId()).isEqualTo(DEFAULT_ORGANISATION_ID);
    }

    @Test
    public void testUpdateBridgeStatus() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.PREPARING);

        // Emulate Shard setting Bridge status to PROVISIONING
        bridge.setStatus(PROVISIONING);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        assertThat(bridgesService.findByShardIdWithReadyDependencies(SHARD_ID)).isEmpty();

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(PROVISIONING);
    }

    @Test
    public void testUpdateBridgeStatusReadyPublishedAt() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        // Emulate Shard setting Bridge status to PROVISIONING
        bridge.setStatus(PROVISIONING);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(retrievedBridge.getStatus()).isEqualTo(PROVISIONING);
        assertThat(retrievedBridge.getPublishedAt()).isNull();

        // Once ready it should have its published date set
        bridge.setStatus(READY);
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        Bridge publishedBridge = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(publishedBridge.getStatus()).isEqualTo(READY);
        ZonedDateTime publishedAt = publishedBridge.getPublishedAt();
        assertThat(publishedAt).isNotNull();

        //Check calls to set PublishedAt at idempotent
        bridgesService.updateBridge(bridgesService.toDTO(bridge));

        Bridge publishedBridge2 = bridgesService.getBridge(bridge.getId(), DEFAULT_CUSTOMER_ID);
        assertThat(publishedBridge2.getStatus()).isEqualTo(READY);
        assertThat(publishedBridge2.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    public void getBridge() {
        BridgeRequest request = new BridgeRequest(DEFAULT_BRIDGE_NAME);
        bridgesService.createBridge(DEFAULT_CUSTOMER_ID, DEFAULT_ORGANISATION_ID, DEFAULT_USER_NAME, request);

        //Wait for Workers to complete
        Bridge bridge = TestUtils.waitForBridgeToBeReady(bridgesService);

        Bridge found = bridgesService.getBridge(bridge.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(bridge.getId());
    }

    @Test
    public void getBridge_bridgeDoesNotExist() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> bridgesService.getBridge("foo"));
    }

    @Test
    public void testDeleteBridge() {
        Bridge bridge = createPersistBridge(READY);

        bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(DEPROVISION);
        assertThat(retrievedBridge.getDeletionRequestedAt()).isNotNull();
    }

    @Test
    public void testDeleteBridge_whenStatusIsFailed() {
        Bridge bridge = createPersistBridge(ManagedResourceStatus.FAILED);

        bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId());

        Bridge retrievedBridge = bridgesService.getBridge(bridge.getId(), bridge.getCustomerId());
        assertThat(retrievedBridge.getStatus()).isEqualTo(DEPROVISION);
        assertThat(retrievedBridge.getDeletionRequestedAt()).isNotNull();
    }

    @Test
    public void testDeleteBridge_whenStatusIsNotReady() {
        Bridge bridge = createPersistBridge(PROVISIONING);
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> bridgesService.deleteBridge(bridge.getId(), bridge.getCustomerId()));
    }

    private Bridge createPersistBridge(ManagedResourceStatus status) {
        Bridge b = Fixtures.createBridge();
        b.setStatus(status);
        bridgeDAO.persist(b);
        return b;
    }
}

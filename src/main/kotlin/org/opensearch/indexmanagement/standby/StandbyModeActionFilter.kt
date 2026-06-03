/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement.standby

import org.apache.logging.log4j.LogManager
import org.opensearch.OpenSearchStatusException
import org.opensearch.action.ActionRequest
import org.opensearch.action.support.ActionFilter
import org.opensearch.action.support.ActionFilterChain
import org.opensearch.action.support.ActionRequestMetadata
import org.opensearch.core.action.ActionListener
import org.opensearch.core.action.ActionResponse
import org.opensearch.core.rest.RestStatus
import org.opensearch.indexmanagement.controlcenter.notification.action.delete.DeleteLRONConfigAction
import org.opensearch.indexmanagement.controlcenter.notification.action.index.IndexLRONConfigAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.addpolicy.AddPolicyAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.changepolicy.ChangePolicyAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.deletepolicy.DeletePolicyAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.indexpolicy.IndexPolicyAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.managedIndex.ManagedIndexAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.removepolicy.RemovePolicyAction
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.retryfailedmanagedindex.RetryFailedManagedIndexAction
import org.opensearch.indexmanagement.rollup.action.delete.DeleteRollupAction
import org.opensearch.indexmanagement.rollup.action.index.IndexRollupAction
import org.opensearch.indexmanagement.rollup.action.start.StartRollupAction
import org.opensearch.indexmanagement.rollup.action.stop.StopRollupAction
import org.opensearch.indexmanagement.snapshotmanagement.api.transport.SMActions
import org.opensearch.indexmanagement.transform.action.delete.DeleteTransformsAction
import org.opensearch.indexmanagement.transform.action.index.IndexTransformAction
import org.opensearch.indexmanagement.transform.action.start.StartTransformAction
import org.opensearch.indexmanagement.transform.action.stop.StopTransformAction
import org.opensearch.tasks.Task
import java.util.function.Supplier

class StandbyModeActionFilter(
    private val standbyModeEnabled: Supplier<Boolean>,
) : ActionFilter {
    private val logger = LogManager.getLogger(javaClass)

    override fun order() = Integer.MIN_VALUE

    override fun <Request : ActionRequest, Response : ActionResponse> apply(
        task: Task,
        action: String,
        request: Request,
        actionRequestMetadata: ActionRequestMetadata<Request, Response>,
        listener: ActionListener<Response>,
        chain: ActionFilterChain<Request, Response>,
    ) {
        if (standbyModeEnabled.get() && MUTATING_ACTIONS.contains(action)) {
            logger.debug("Index Management standby mode is enabled, rejecting mutating action [{}].", action)
            listener.onFailure(
                OpenSearchStatusException(
                    "Index Management is read-only because this cluster is in standby mode.",
                    RestStatus.FORBIDDEN,
                ),
            )
            return
        }

        chain.proceed(task, action, request, listener)
    }

    companion object {
        val MUTATING_ACTIONS = setOf(
            AddPolicyAction.NAME,
            ChangePolicyAction.NAME,
            DeletePolicyAction.NAME,
            IndexPolicyAction.NAME,
            ManagedIndexAction.NAME,
            RemovePolicyAction.NAME,
            RetryFailedManagedIndexAction.NAME,
            DeleteRollupAction.NAME,
            IndexRollupAction.NAME,
            StartRollupAction.NAME,
            StopRollupAction.NAME,
            SMActions.DELETE_SM_POLICY_ACTION_NAME,
            SMActions.INDEX_SM_POLICY_ACTION_NAME,
            SMActions.START_SM_POLICY_ACTION_NAME,
            SMActions.STOP_SM_POLICY_ACTION_NAME,
            DeleteTransformsAction.NAME,
            IndexTransformAction.NAME,
            StartTransformAction.NAME,
            StopTransformAction.NAME,
            DeleteLRONConfigAction.NAME,
            IndexLRONConfigAction.NAME,
        )
    }
}

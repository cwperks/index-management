/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement.standby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.mockito.Mockito
import org.opensearch.OpenSearchStatusException
import org.opensearch.action.ActionRequest
import org.opensearch.action.admin.cluster.node.info.NodesInfoAction
import org.opensearch.action.support.ActionFilterChain
import org.opensearch.action.support.ActionRequestMetadata
import org.opensearch.core.action.ActionListener
import org.opensearch.core.action.ActionResponse
import org.opensearch.core.rest.RestStatus
import org.opensearch.indexmanagement.indexstatemanagement.transport.action.indexpolicy.IndexPolicyAction
import org.opensearch.tasks.Task
import org.opensearch.test.OpenSearchTestCase

class StandbyModeActionFilterTests : OpenSearchTestCase() {
    fun `test rejects mutating action in standby mode`() {
        val filter = StandbyModeActionFilter { true }
        val listener = CapturingActionListener<ActionResponse>()

        filter.apply(
            Mockito.mock(Task::class.java),
            IndexPolicyAction.NAME,
            Mockito.mock(ActionRequest::class.java),
            actionRequestMetadata(),
            listener,
            actionFilterChain(),
        )

        val failure = listener.failure as OpenSearchStatusException
        assertEquals(RestStatus.FORBIDDEN, failure.status())
    }

    fun `test proceeds for read action in standby mode`() {
        val filter = StandbyModeActionFilter { true }
        val listener = CapturingActionListener<ActionResponse>()
        val chain = CapturingActionFilterChain()
        val request = Mockito.mock(ActionRequest::class.java)

        filter.apply(
            Mockito.mock(Task::class.java),
            NodesInfoAction.NAME,
            request,
            actionRequestMetadata(),
            listener,
            chain,
        )

        assertNull(listener.failure)
        assertSame(request, chain.request)
    }

    @Suppress("UNCHECKED_CAST")
    private fun actionRequestMetadata(): ActionRequestMetadata<ActionRequest, ActionResponse> =
        Mockito.mock(ActionRequestMetadata::class.java) as ActionRequestMetadata<ActionRequest, ActionResponse>

    @Suppress("UNCHECKED_CAST")
    private fun actionFilterChain(): ActionFilterChain<ActionRequest, ActionResponse> =
        Mockito.mock(ActionFilterChain::class.java) as ActionFilterChain<ActionRequest, ActionResponse>

    private class CapturingActionListener<Response : ActionResponse> : ActionListener<Response> {
        var failure: Exception? = null

        override fun onResponse(response: Response) {}

        override fun onFailure(e: Exception) {
            failure = e
        }
    }

    private class CapturingActionFilterChain : ActionFilterChain<ActionRequest, ActionResponse> {
        var request: ActionRequest? = null

        override fun proceed(
            task: Task,
            action: String,
            request: ActionRequest,
            listener: ActionListener<ActionResponse>,
        ) {
            this.request = request
        }
    }
}

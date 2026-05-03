/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.indexmanagement

import org.mockito.Mockito
import org.opensearch.jobscheduler.spi.JobExecutionContext
import org.opensearch.jobscheduler.spi.ScheduledJobParameter
import org.opensearch.test.OpenSearchTestCase
import java.util.concurrent.atomic.AtomicBoolean

class IndexManagementRunnerTests : OpenSearchTestCase() {
    fun `test run job returns early in standby mode`() {
        val standbyModeEnabled = AtomicBoolean(true)
        IndexManagementRunner.registerStandbyModeSupplier(standbyModeEnabled::get)

        val job = Mockito.mock(ScheduledJobParameter::class.java)
        val context = Mockito.mock(JobExecutionContext::class.java)
        Mockito.`when`(context.jobId).thenReturn("standby-job")

        IndexManagementRunner.runJob(job, context)
    }

    override fun tearDown() {
        IndexManagementRunner.registerStandbyModeSupplier { false }
        super.tearDown()
    }
}

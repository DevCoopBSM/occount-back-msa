package devcoop.occount.item.api.aripick

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.item.application.query.AripickListResponse
import devcoop.occount.item.application.query.AripickQueryService
import devcoop.occount.item.application.query.AripickStatsResponse
import devcoop.occount.item.application.shared.AripickResponse
import devcoop.occount.item.application.usecase.aripick.AripickCommandUseCase
import devcoop.occount.item.application.usecase.aripick.AripickLikeToggleResponse
import devcoop.occount.item.application.usecase.aripick.CreateAripickRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/ari-pick")
@RestController
class AripickController(
    private val aripickQueryService: AripickQueryService,
    private val aripickCommandUseCase: AripickCommandUseCase,
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAripickItems(): AripickListResponse {
        return aripickQueryService.getAllItems()
    }

    @GetMapping("/{proposalId}")
    @ResponseStatus(HttpStatus.OK)
    fun getAripickItem(
        @PathVariable proposalId: Long,
    ): AripickResponse {
        return aripickQueryService.getItem(proposalId)
    }

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    fun getAripickStats(): AripickStatsResponse {
        return aripickQueryService.getStats()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAripickItem(
        @RequestBody request: CreateAripickRequest,
        httpRequest: HttpServletRequest,
    ): AripickResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return aripickCommandUseCase.create(request, userId)
    }

    @PatchMapping("/{proposalId}/approve")
    @ResponseStatus(HttpStatus.OK)
    fun approveAripickItem(
        @PathVariable proposalId: Long,
    ): AripickResponse {
        return aripickCommandUseCase.approve(proposalId)
    }

    @PatchMapping("/{proposalId}/reject")
    @ResponseStatus(HttpStatus.OK)
    fun rejectAripickItem(
        @PathVariable proposalId: Long,
    ): AripickResponse {
        return aripickCommandUseCase.reject(proposalId)
    }

    @PatchMapping("/{proposalId}/pending")
    @ResponseStatus(HttpStatus.OK)
    fun pendingAripickItem(
        @PathVariable proposalId: Long,
    ): AripickResponse {
        return aripickCommandUseCase.pending(proposalId)
    }

    @DeleteMapping("/{proposalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAripickItem(
        @PathVariable proposalId: Long,
        httpRequest: HttpServletRequest,
    ) {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        aripickCommandUseCase.delete(proposalId, userId)
    }

    @DeleteMapping("/{proposalId}/admin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAripickItemAsAdmin(
        @PathVariable proposalId: Long,
    ) {
        aripickCommandUseCase.deleteAsAdmin(proposalId)
    }

    @PostMapping("/{proposalId}/like")
    @ResponseStatus(HttpStatus.OK)
    fun toggleLike(
        @PathVariable proposalId: Long,
        httpRequest: HttpServletRequest,
    ): AripickLikeToggleResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return aripickCommandUseCase.toggleLike(proposalId, userId)
    }
}

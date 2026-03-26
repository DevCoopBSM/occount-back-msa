package devcoop.occount.order.api.order

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.order.application.order.OrderService
import devcoop.occount.order.application.order.OrderRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun handleOrder(@RequestBody orderRequest: OrderRequest, httpServletRequest: HttpServletRequest) {
        val authPrincipal = RequestAuthPrincipalResolver.resolve(httpServletRequest)
        orderService.order(orderRequest, authPrincipal.userId)
    }
}

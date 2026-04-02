package devcoop.occount.member.application.output

interface WalletPointReader {
    fun getPoint(userId: Long): Int
}

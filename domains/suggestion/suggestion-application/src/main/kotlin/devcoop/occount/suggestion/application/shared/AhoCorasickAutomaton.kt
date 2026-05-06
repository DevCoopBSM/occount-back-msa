package devcoop.occount.suggestion.application.shared

import java.util.ArrayDeque
import java.util.Locale

class AhoCorasickAutomaton private constructor(
    private val root: Node,
) {
    fun contains(text: String): Boolean {
        val normalizedText = text.lowercase(Locale.ROOT)
        var state = root
        for (char in normalizedText) {
            while (state !== root && !state.children.containsKey(char)) {
                state = state.fail
            }
            state = state.children[char] ?: root
            if (state.isTerminal) {
                return true
            }
        }
        return false
    }

    private class Node(
        val children: MutableMap<Char, Node> = mutableMapOf(),
        var isTerminal: Boolean = false,
    ) {
        lateinit var fail: Node
    }

    companion object {
        fun build(keywords: List<String>): AhoCorasickAutomaton {
            val normalizedKeywords = keywords.asSequence()
                .map { it.trim().lowercase(Locale.ROOT) }
                .filter { it.isNotEmpty() }
                .toList()
            if (normalizedKeywords.isEmpty()) {
                return empty()
            }

            val root = Node()
            root.fail = root

            for (pattern in normalizedKeywords) {
                var node = root
                for (char in pattern) {
                    node = node.children.getOrPut(char) {
                        Node().apply { fail = root }
                    }
                }
                node.isTerminal = true
            }

            val queue = ArrayDeque<Node>()
            for (child in root.children.values) {
                child.fail = root
                queue.addLast(child)
            }

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                for ((char, next) in current.children) {
                    var failure = current.fail
                    while (failure !== root && !failure.children.containsKey(char)) {
                        failure = failure.fail
                    }
                    next.fail = failure.children[char] ?: root
                    next.isTerminal = next.isTerminal || next.fail.isTerminal
                    queue.addLast(next)
                }
            }

            return AhoCorasickAutomaton(root)
        }

        fun empty(): AhoCorasickAutomaton {
            val root = Node()
            root.fail = root
            return AhoCorasickAutomaton(root)
        }
    }
}

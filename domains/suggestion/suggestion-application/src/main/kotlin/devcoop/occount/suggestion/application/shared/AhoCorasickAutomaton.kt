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
            state = state.transitions[char] ?: root
            if (state.isTerminal) {
                return true
            }
        }
        return false
    }

    private class Node(
        val edges: MutableMap<Char, Node> = mutableMapOf(),
        val transitions: MutableMap<Char, Node> = mutableMapOf(),
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
            val alphabet = linkedSetOf<Char>()

            for (pattern in normalizedKeywords) {
                var node = root
                for (char in pattern) {
                    alphabet += char
                    node = node.edges.getOrPut(char) {
                        Node().apply { fail = root }
                    }
                }
                node.isTerminal = true
            }

            val queue = ArrayDeque<Node>()
            for (char in alphabet) {
                root.transitions[char] = root.edges[char] ?: root
            }

            for (child in root.edges.values) {
                child.fail = root
                queue.addLast(child)
            }

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                for ((char, next) in current.edges) {
                    var failure = current.fail
                    while (failure !== root && !failure.edges.containsKey(char)) {
                        failure = failure.fail
                    }
                    next.fail = failure.edges[char] ?: root
                    next.isTerminal = next.isTerminal || next.fail.isTerminal
                    queue.addLast(next)
                }

                for (char in alphabet) {
                    current.transitions[char] = current.edges[char]
                        ?: current.fail.transitions[char]
                        ?: root
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

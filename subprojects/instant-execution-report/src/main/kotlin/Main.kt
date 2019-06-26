/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import data.Trie
import data.trieFrom
import elmish.elementById
import elmish.mountComponentAt
import elmish.tree.Tree
import elmish.tree.TreeView


fun main() {
    mountComponentAt(
        elementById("app"),
        HomePage,
        homePageModelFromJsModel(instantExecutionFailures)
    )
}


/**
 * External model defined in `instant-execution-failures.js`, a file generated by `InstantExecutionReport`.
 */
private
external val instantExecutionFailures: JsModel


private
typealias JsModel = Array<JsFailure>


private
external interface JsFailure {
    val trace: Array<String>
    val message: String
    val error: String?
}


private
fun homePageModelFromJsModel(failures: JsModel) = HomePage.Model(
    totalFailures = instantExecutionFailures.size,
    messageTree = treeModelFor(
        FailureNode.Label("Failures grouped by message"),
        failureNodesByMessage(failures)
    ),
    taskTree = treeModelFor(
        FailureNode.Label("Failures grouped by task"),
        failureNodesByTask(failures)
    )
)


private
fun failureNodesByTask(failures: JsModel): Sequence<List<FailureNode>> =
    failures.asSequence().map { failure ->
        failure.trace.asList().asReversed().map(FailureNode::Label) + failureNodeFor(failure)
    }


private
fun failureNodesByMessage(failures: JsModel): Sequence<MutableList<FailureNode>> =
    failures.asSequence().map { failure ->
        mutableListOf<FailureNode>().apply {
            add(FailureNode.Label(failure.message))
            failure.trace.forEach { part ->
                add(FailureNode.Label(part))
            }
            errorNodeFor(failure)?.let {
                add(it)
            }
        }
    }


private
fun failureNodeFor(it: JsFailure) =
    errorNodeFor(it) ?: FailureNode.Label(it.message)


private
fun errorNodeFor(it: JsFailure): FailureNode? =
    it.error?.let {
        val stackTraceLines = it.lineSequence()
        FailureNode.Error(
            stackTraceLines.first(),
            stackTraceLines.drop(1).map(String::trim).joinToString("\n")
        )
    }


private
fun <T> treeModelFor(
    label: T,
    sequence: Sequence<List<T>>
): TreeView.Model<T> = TreeView.Model(
    treeFromTrie(
        label,
        trieFrom(sequence)
    )
)


private
fun <T> treeFromTrie(label: T, trie: Trie<T>): Tree<T> =
    Tree(label, subTreesFromTrie(trie))


private
fun <T> subTreesFromTrie(trie: Trie<T>): List<Tree<T>> =
    trie.asSequence().sortedBy { it.key.toString() /* TODO: */ }.map {
        @Suppress("unchecked_cast")
        treeFromTrie(
            it.key,
            it.value as Trie<T>
        )
    }.toList()

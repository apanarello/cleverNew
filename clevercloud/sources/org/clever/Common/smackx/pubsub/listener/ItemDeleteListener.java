/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.clever.Common.smackx.pubsub.listener;

import org.clever.Common.smackx.pubsub.ItemDeleteEvent;
import org.clever.Common.smackx.pubsub.LeafNode;

/**
 * Defines the listener for item deletion events from a node.
 * 
 * @see LeafNode#addItemDeleteListener(ItemDeleteListener)
 * 
 * @author Robin Collier
 */
public interface ItemDeleteListener
{
	/**
	 * Called when items are deleted from a node the listener is 
	 * registered with.
	 * 
	 * @param items The event with item deletion details
	 */
	void handleDeletedItems(ItemDeleteEvent items);
	
	/**
	 * Called when <b>all</b> items are deleted from a node the listener is 
	 * registered with. 
	 */
	void handlePurge();
}

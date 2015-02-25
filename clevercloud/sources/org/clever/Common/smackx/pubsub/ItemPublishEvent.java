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
package org.clever.Common.smackx.pubsub;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents an event generated by an item(s) being published to a node.
 * 
 * @author Robin Collier
 */
public class ItemPublishEvent <T extends Item> extends SubscriptionEvent
{
	private List<T> items;
	private Date originalDate;
	
	/**
	 * Constructs an <tt>ItemPublishEvent</tt> with the provided list
	 * of {@link Item} that were published.
	 * 
	 * @param nodeId The id of the node the event came from
	 * @param eventItems The list of {@link Item} that were published 
	 */
	public ItemPublishEvent(String nodeId, List<T> eventItems)
	{
		super(nodeId);
		items = eventItems;
	}
	
	/**
	 * Constructs an <tt>ItemPublishEvent</tt> with the provided list
	 * of {@link Item} that were published.  The list of subscription ids
	 * represents the subscriptions that matched the event, in the case 
	 * of the user having multiple subscriptions.
	 * 
	 * @param nodeId The id of the node the event came from
	 * @param eventItems The list of {@link Item} that were published 
	 * @param subscriptionIds The list of subscriptionIds
	 */
	public ItemPublishEvent(String nodeId, List<T> eventItems, List<String> subscriptionIds)
	{
		super(nodeId, subscriptionIds);
		items = eventItems;
	}
	
	/**
	 * Constructs an <tt>ItemPublishEvent</tt> with the provided list
	 * of {@link Item} that were published in the past.  The published
	 * date signifies that this is delayed event.  The list of subscription ids
	 * represents the subscriptions that matched the event, in the case 
	 * of the user having multiple subscriptions. 
	 *
	 * @param nodeId The id of the node the event came from
	 * @param eventItems The list of {@link Item} that were published 
	 * @param subscriptionIds The list of subscriptionIds
	 * @param publishedDate The original publishing date of the events
	 */
	public ItemPublishEvent(String nodeId, List<T> eventItems, List<String> subscriptionIds, Date publishedDate)
	{
		super(nodeId, subscriptionIds);
		items = eventItems;
		
		if (publishedDate != null)
			originalDate = publishedDate;
	}
	
	/**
	 * Get the list of {@link Item} that were published.
	 * 
	 * @return The list of published {@link Item}
	 */
	public List<T> getItems()
	{
		return Collections.unmodifiableList(items);
	}
	
	/**
	 * Indicates whether this event was delayed.  That is, the items
	 * were published to the node at some time in the past.  This will 
	 * typically happen if there is an item already published to the node
	 * before a user subscribes to it.  In this case, when the user 
	 * subscribes, the server may send the last item published to the node
	 * with a delay date showing its time of original publication.
	 * 
	 * @return true if the items are delayed, false otherwise.
	 */
	public boolean isDelayed()
	{
		return (originalDate != null);
	}
	
	/**
	 * Gets the original date the items were published.  This is only 
	 * valid if {@link #isDelayed()} is true.
	 * 
	 * @return Date items were published if {@link #isDelayed()} is true, null otherwise.
	 */
	public Date getPublishedDate()
	{
		return originalDate;
	}

	@Override
	public String toString()
	{
		return getClass().getName() + "  [subscriptions: " + getSubscriptions() + "], [Delayed: " + 
			(isDelayed() ? originalDate.toString() : "false") + ']';
	}
	
}

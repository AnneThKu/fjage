/******************************************************************************

Copyright (c) 2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

#ifndef _FJAGE_H_
#define _FJAGE_H_

#include <stdbool.h>
#include <stdint.h>

typedef void* fjage_gw_t;         ///< fjåge Gateway.
typedef char* fjage_aid_t;        ///< fjåge AgentID.
typedef void* fjage_msg_t;        ///< fjåge Message.

/// Message performatives.
typedef enum {
  FJAGE_NONE = 0,
  FJAGE_REQUEST = 1,
  FJAGE_AGREE = 2,
  FJAGE_REFUSE = 3,
  FJAGE_FAILURE = 4,
  FJAGE_INFORM = 5,
  FJAGE_CONFIRM = 6,
  FJAGE_DISCONFIRM = 7,
  FJAGE_QUERY_IF = 8,
  FJAGE_NOT_UNDERSTOOD = 9,
  FJAGE_CFP = 10,
  FJAGE_PROPOSE = 11,
  FJAGE_CANCEL = 12
} fjage_perf_t;

/// Open a gateway to a fjåge master container via TCP/IP.
///
/// @param hostname       Host name or IP address
/// @param port           TCP port number
/// @return               Gateway

fjage_gw_t fjage_tcp_open(const char* hostname, int port);

/// Close a gateway to a fjåge master container. Once a gateway is closed,
/// the gateway object is invalid and should no longer be used.
///
/// @param gw             Gateway
/// @return               0 on success, error code otherwise

int fjage_close(fjage_gw_t gw);

/// Get the AgentID of the gateway. A gateway appears as a single agent in
/// a fjåge slave container. The AgentID returned by this function should not be
/// freed by the caller.
///
/// @param gw             Gateway
/// @return               The AgentID of the gateway agent

fjage_aid_t fjage_get_agent_id(fjage_gw_t gw);

/// Subscribe to a topic.
///
/// @param gw             Gateway
/// @param topic          Topic to subscribe to, usually generated using fjage_aid_topic()
/// @return               0 on success, error code otherwise

int fjage_subscribe(fjage_gw_t gw, const fjage_aid_t topic);

/// Unsubscribe from a topic.
///
/// @param gw             Gateway
/// @param topic          Topic to subscribe to, usually generated using fjage_aid_topic()
/// @return               0 on success, error code otherwise

int fjage_unsubscribe(fjage_gw_t gw, const fjage_aid_t topic);

/// Check if a topic is subscribed to.
///
/// @param gw             Gateway
/// @param topic          Topic to check, usually generated using fjage_aid_topic()
/// @return               true if subscribed to, false otherwise

bool fjage_is_subscribed(fjage_gw_t gw, const fjage_aid_t topic);

/// Find an agent providing a specified service. The AgentID returned by this function
/// should be freed by the caller using fjage_aid_destroy().
///
/// @param gw             Gateway
/// @param service        Fully qualified name of a service
/// @return               AgentID of an agent providing the service, NULL if none found

fjage_aid_t fjage_agent_for_service(fjage_gw_t gw, const char* service);

/// Find all agents providing a specified service. The list of agents is populated in an
/// array provided by the caller. If only the number of agents is desired, a NULL may be
/// passed in instead of an array, and max can be set to 0. All AgentIDs returned by this
/// function should be freed by the caller using fjage_aid_destroy().
///
/// @param gw             Gateway
/// @param service        Fully qualified name of a service
/// @param agents         An array of AgentIDs for the function to fill, or NULL
/// @param max            Size of the agents array, or 0 if agents is NULL
/// @return               Number of agents providing the service

int fjage_agents_for_service(fjage_gw_t gw, const char* service, fjage_aid_t* agents, int max);

/// Send a message. The message object passed in is considered consumed after this call,
/// and should not be used or freed by the caller.
///
/// @param gw             Gateway
/// @param msg            Message to send
/// @return               0 on success, error code otherwise

int fjage_send(fjage_gw_t gw, const fjage_msg_t msg);

/// Receive a message. The received message should be freed by the caller using fjage_msg_destroy().
/// If clazz is not NULL, only messages of a specified message class are recevied. If id is not
/// NULL, only messages that are in response to the message specified by the id are received.
///
/// Recevied messages are open in read-only mode, where the getter fjage_msg_get_* functions may
/// be called, but not the setters.
///
/// @param gw             Gateway
/// @param clazz          Fully qualified name of message class, or NULL
/// @param id             MessageID of the message being responded to, or NULL
/// @param timeout        Timeout in milliseconds
/// @return               The received message in read-only mode, or NULL on timeout

fjage_msg_t fjage_receive(fjage_gw_t gw, const char* clazz, const char* id, long timeout);

/// Send a message and wait for a response. The message object passed in is considered consumed
/// after the call, and should not be used or freed by the caller. If a response is returned,
/// it should be freed by the caller using fjage_msg_destroy().
///
/// Recevied messages are open in read-only mode, where the getter fjage_msg_get_* functions may
/// be called, but not the setters.
///
/// @param gw             Gateway
/// @param request        Request message to send
/// @param timeout        Timeout in milliseconds
/// @return               Response message in read-only mode, or NULL on timeout

fjage_msg_t fjage_request(fjage_gw_t gw, const fjage_msg_t request, long timeout);

/// Create an AgentID. The AgentID created using this function should be freed using
/// fjage_aid_destroy().
///
/// @param name           Name of the agent
/// @return               AgentID

fjage_aid_t fjage_aid_create(const char* name);

/// Create an topic. The topic AgentID created using this function should be freed using
/// fjage_aid_destroy().
///
/// @param name           Name of the topic
/// @return               AgentID for the specified topic

fjage_aid_t fjage_aid_topic(const char* topic);

/// Destroy an AgentID. Once destroyed, the AgentID is considered invalid and should no
/// longer be used.
///
/// @param aid            AgentID to destroy

void fjage_aid_destroy(fjage_aid_t aid);

/// Creates a new message. New messages are open in write-only mode for eventual sending.
/// Getters of the message should not be called. Only fjage_msg_set_* and fjage_msg_add_*
/// functions should be called on the message. If the message is eventually not
/// sent, it may be destroyed using fjage_msg_destroy().
///
/// @param clazz          Fully qualified message class
/// @param perf           Performative of the message
/// @return               Message open in write-only mode

fjage_msg_t fjage_msg_create(const char* clazz, fjage_perf_t perf);

/// Destroy a message. Once destroyed, the message is considered invalid and should
/// no longer be used.
///
/// @param msg            Message to destroy

void fjage_msg_destroy(fjage_msg_t msg);

/// Set the recipient of a message.
///
/// @param msg            Message in write-only mode
/// @param aid            AgentID of the recipient

void fjage_msg_set_recipient(fjage_msg_t msg, fjage_aid_t aid);

/// Set the message ID of the request which is being responded to.
///
/// @param msg            Message in write-only mode
/// @param id             Message ID of the request being responded to

void fjage_msg_set_in_reply_to(fjage_msg_t msg, const char* id);

/// Add a string value to a message.
///
/// @param msg            Message in write-only mode
/// @param key            Key
/// @param value          Value

void fjage_msg_add_string(fjage_msg_t msg, const char* key, const char* value);

/// Add an integer value to a message.
///
/// @param msg            Message in write-only mode
/// @param key            Key
/// @param value          Value

void fjage_msg_add_int(fjage_msg_t msg, const char* key, int value);

/// Add a floating point value to a message.
///
/// @param msg            Message in write-only mode
/// @param key            Key
/// @param value          Value

void fjage_msg_add_float(fjage_msg_t msg, const char* key, float value);

/// Add a boolean value to a message.
///
/// @param msg            Message in write-only mode
/// @param key            Key
/// @param value          Value

void fjage_msg_add_bool(fjage_msg_t msg, const char* key, bool value);

/// Add a byte array value to a message.
///
/// @param msg            Message in write-only mode
/// @param key            Key
/// @param value          Pointer to the byte array
/// @param len            Length of the byte array

void fjage_msg_add_byte_array(fjage_msg_t msg, const char* key, uint8_t* value, int len);

/// Add a floating point array value to a message.
///
/// @param msg            Message in write-only mode
/// @param key            Key
/// @param value          Pointer to the floating point array
/// @param len            Length of the array (in floats)

void fjage_msg_add_float_array(fjage_msg_t msg, const char* key, float* value, int len);

/// Get the message ID. The string returned by this function should
/// not be freed by the caller. However, it will be invalid after the message
/// is destroyed.
///
/// @param msg            Message in read-only mode
/// @return               Message ID

const char* fjage_msg_get_id(fjage_msg_t msg);

/// Get the message class. The string returned by this function should
/// not be freed by the caller. However, it will be invalid after the message
/// is destroyed.
///
/// @param msg            Message in read-only mode
/// @return               Fully qualified message class name

const char* fjage_msg_get_clazz(fjage_msg_t msg);

/// Get the message performative.
///
/// @param msg            Message in read-only mode
/// @return               Message performative

fjage_perf_t fjage_msg_get_performative(fjage_msg_t msg);

/// Get the message recipient. The AgentID returned by this function should
/// not be freed by the caller. However, it will be invalid after the message
/// is destroyed.
///
/// @param msg            Message in read-only mode
/// @return               AgentID of the recipient

fjage_aid_t fjage_msg_get_recipient(fjage_msg_t msg);

/// Get the message sender. The AgentID returned by this function should
/// not be freed by the caller. However, it will be invalid after the message
/// is destroyed.
///
/// @param msg            Message in read-only mode
/// @return               AgentID of the sender

fjage_aid_t fjage_msg_get_sender(fjage_msg_t msg);

/// Get the message ID of the request corresponding to this response.
/// The string returned by this function should not be freed by the caller.
/// However, it will be invalid after the message is destroyed.
///
/// @param msg            Message in read-only mode
/// @return               Message ID of the corresponding request

const char* fjage_msg_get_in_reply_to(fjage_msg_t msg);

/// Get a string value. The string returned by this function should not
/// be freed by the caller. However, it will be invalid after the message
/// is destroyed.
///
/// @param msg            Message in read-only mode
/// @param key            Key
/// @return               String value

const char* fjage_msg_get_string(fjage_msg_t msg, const char* key);

/// Get an integer value.
///
/// @param msg            Message in read-only mode
/// @param key            Key
/// @return               Integer value

int fjage_msg_get_int(fjage_msg_t msg, const char* key, int defval);

/// Get a floating point value.
///
/// @param msg            Message in read-only mode
/// @param key            Key
/// @return               Floating point value

float fjage_msg_get_float(fjage_msg_t msg, const char* key, float defval);

/// Get a boolean value.
///
/// @param msg            Message in read-only mode
/// @param key            Key
/// @return               Boolean value

bool fjage_msg_get_bool(fjage_msg_t msg, const char* key, bool defval);

/// Get a byte array value. If only the length of the array is desired (so that
/// an array can be allocated), passing NULL as value and 0 as maxlen returns
/// the array length.
///
/// @param msg            Message in read-only mode
/// @param key            Key
/// @param value          Pointer to a byte array to receive data, or NULL
/// @param maxlen         The maximum number of bytes to receive, or 0 if value is NULL
/// @return               Number of bytes in the byte array

int fjage_msg_get_byte_array(fjage_msg_t msg, const char* key, uint8_t* value, int maxlen);

/// Get a floating point array value. If only the length of the array is desired (so that
/// an array can be allocated), passing NULL as value and 0 as maxlen returns
/// the array length.
///
/// @param msg            Message in read-only mode
/// @param key            Key
/// @param value          Pointer to a floating point array to receive data, or NULL
/// @param maxlen         The maximum number of floats to receive, or 0 if value is NULL
/// @return               Number of floats in the array

int fjage_msg_get_float_array(fjage_msg_t msg, const char* key, float* value, int maxlen);

#endif

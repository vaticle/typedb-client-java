/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

use std::{collections::HashSet, error::Error as StdError, fmt};

use tonic::{Code, Status};
use typeql::error_messages;

use super::{address::Address, RequestID};

error_messages! { ConnectionError
    code: "CXN", type: "Connection Error",
    RPCMethodUnavailable { message: String } =
        1: "The server does not support this method, please check the driver-server compatibility:\n'{message}'.",
    ConnectionIsClosed =
        2: "The connection has been closed and no further operation is allowed.",
    SessionIsClosed =
        3: "The session is closed and no further operation is allowed.",
    TransactionIsClosed =
        4: "The transaction is closed and no further operation is allowed.",
    TransactionIsClosedWithErrors { errors: String } =
        5: "The transaction is closed because of the error(s):\n{errors}",
    DatabaseDoesNotExist { name: String } =
        6: "The database '{name}' does not exist.",
    MissingResponseField { field: &'static str } =
        7: "Missing field in message received from server: '{field}'.",
    UnknownRequestId { request_id: RequestID } =
        8: "Received a response with unknown request id '{request_id}'",
    InvalidResponseField { name: &'static str } =
        9: "Invalid field in message received from server: '{name}'.",
    UnexpectedResponse { response: String } =
        10: "Received unexpected response from server: '{response}'.",
    ServerConnectionFailed { addresses: Vec<Address> } =
        11: "Unable to connect to TypeDB server(s) at: \n{addresses:?}",
    ServerConnectionFailedWithError { error: String } =
        12: "Unable to connect to TypeDB server(s), received errors: \n{error}",
    ServerConnectionFailedStatusError { error: String } =
        13: "Unable to connect to TypeDB server(s), received network error: \n{error}",
    UserManagementCloudOnly =
        14: "User management is only available in TypeDB Cloud servers.",
    CloudReplicaNotPrimary =
        15: "The replica is not the primary replica.",
    CloudAllNodesFailed { errors: String } =
        16: "Attempted connecting to all TypeDB Cloud servers, but the following errors occurred: \n{errors}.",
    CloudTokenCredentialInvalid =
        17: "Invalid token credential.",
    SessionCloseFailed =
        18: "Failed to close session. It may still be open on the server: or it may already have been closed previously.",
    CloudEncryptionSettingsMismatch =
        19: "Unable to connect to TypeDB Cloud: possible encryption settings mismatch.",
    CloudSSLCertificateNotValidated =
        20: "SSL handshake with TypeDB Cloud failed: the server's identity could not be verified. Possible CA mismatch.",
    BrokenPipe =
        21: "Stream closed because of a broken pipe. This could happen if you are attempting to connect to an unencrypted cloud instance using a TLS-enabled credential.",
    ConnectionFailed =
        22: "Connection failed. Please check the server is running and the address is accessible. Encrypted Cloud endpoints may also have misconfigured SSL certificates.",
    MissingPort { address: String } =
        23: "Invalid URL '{address}': missing port.",
    AddressTranslationMismatch { unknown: HashSet<Address>, unmapped: HashSet<Address> } =
        24: "Address translation map does not match the server's advertised address list. User-provided servers not in the advertised list: {unknown:?}. Advertised servers not mapped by user: {unmapped:?}.",
    ConnectionTimedOut =
        25: "Connection to the server timed out."
}

error_messages! { InternalError
    code: "INT", type: "Internal Error",
    RecvError =
        1: "Channel is closed.",
    SendError =
        2: "Unable to send response over callback channel (receiver dropped).",
    UnexpectedRequestType { request_type: String } =
        3: "Unexpected request type for remote procedure call: {request_type}.",
    UnexpectedResponseType { response_type: String } =
        4: "Unexpected response type for remote procedure call: {response_type}.",
    UnknownServer { server: Address } =
        5: "Received replica at unrecognized server: {server}.",
    EnumOutOfBounds { value: i32, enum_name: &'static str } =
        6: "Value '{value}' is out of bounds for enum '{enum_name}'.",
}

/// Represents errors encountered during operation.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum Error {
    Connection(ConnectionError),
    Internal(InternalError),
    TypeQL(typeql::common::Error),
    Other(String),
}

impl Error {
    pub fn code(&self) -> String {
        match self {
            Self::Connection(error) => error.format_code(),
            Self::Internal(error) => error.format_code(),
            Self::TypeQL(_error) => String::new(),
            Self::Other(_error) => String::new(),
        }
    }

    pub fn message(&self) -> String {
        match self {
            Self::Connection(error) => error.message(),
            Self::Internal(error) => error.message(),
            Self::TypeQL(error) => error.to_string(),
            Self::Other(error) => error.clone(),
        }
    }

    fn from_message(message: &str) -> Self {
        match message.split_ascii_whitespace().next() {
            Some("[RPL01]") => Self::Connection(ConnectionError::CloudReplicaNotPrimary),
            // TODO: CLS and ENT are synonyms which we can simplify on protocol change
            Some("[CLS08]") => Self::Connection(ConnectionError::CloudTokenCredentialInvalid),
            Some("[ENT08]") => Self::Connection(ConnectionError::CloudTokenCredentialInvalid),
            Some("[DBS06]") => Self::Connection(ConnectionError::DatabaseDoesNotExist {
                name: message.split('\'').nth(1).unwrap_or("{unknown}").to_owned(),
            }),
            _ => Self::Other(message.to_owned()),
        }
    }

    fn parse_unavailable(status_message: &str) -> Error {
        if status_message == "broken pipe" {
            Error::Connection(ConnectionError::BrokenPipe)
        } else if status_message.contains("received corrupt message") {
            Error::Connection(ConnectionError::CloudEncryptionSettingsMismatch)
        } else if status_message.contains("UnknownIssuer") {
            Error::Connection(ConnectionError::CloudSSLCertificateNotValidated)
        } else if status_message.contains("Connection refused") {
            Error::Connection(ConnectionError::ConnectionFailed)
        } else {
            Error::Connection(ConnectionError::ServerConnectionFailedStatusError { error: status_message.to_owned() })
        }
    }
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Connection(error) => write!(f, "{error}"),
            Self::Internal(error) => write!(f, "{error}"),
            Self::TypeQL(error) => write!(f, "{error}"),
            Self::Other(message) => write!(f, "{message}"),
        }
    }
}

impl StdError for Error {
    fn source(&self) -> Option<&(dyn StdError + 'static)> {
        match self {
            Self::Connection(error) => Some(error),
            Self::Internal(error) => Some(error),
            Self::TypeQL(error) => Some(error),
            Self::Other(_) => None,
        }
    }
}

impl From<ConnectionError> for Error {
    fn from(error: ConnectionError) -> Self {
        Self::Connection(error)
    }
}

impl From<InternalError> for Error {
    fn from(error: InternalError) -> Self {
        Self::Internal(error)
    }
}

impl From<typeql::common::Error> for Error {
    fn from(err: typeql::common::Error) -> Self {
        Self::TypeQL(err)
    }
}

impl From<Status> for Error {
    fn from(status: Status) -> Self {
        if status.code() == Code::Unavailable {
            Self::parse_unavailable(status.message())
        } else if status.code() == Code::Unknown || is_rst_stream(&status) {
            Self::Connection(ConnectionError::ServerConnectionFailedStatusError { error: status.message().to_owned() })
        } else if status.code() == Code::Unimplemented {
            Self::Connection(ConnectionError::RPCMethodUnavailable { message: status.message().to_owned() })
        } else if status.code() == Code::Cancelled && status.message() == "Timeout expired" {
            Self::Connection(ConnectionError::ConnectionTimedOut)
        } else {
            Self::from_message(status.message())
        }
    }
}

fn is_rst_stream(status: &Status) -> bool {
    // "Received Rst Stream" occurs if the server is in the process of shutting down.
    status.message().contains("Received Rst Stream")
}

impl From<http::uri::InvalidUri> for Error {
    fn from(err: http::uri::InvalidUri) -> Self {
        Self::Other(err.to_string())
    }
}

impl From<tonic::transport::Error> for Error {
    fn from(err: tonic::transport::Error) -> Self {
        Self::Other(err.to_string())
    }
}

impl<T> From<tokio::sync::mpsc::error::SendError<T>> for Error {
    fn from(_err: tokio::sync::mpsc::error::SendError<T>) -> Self {
        Self::Internal(InternalError::SendError)
    }
}

impl From<tokio::sync::oneshot::error::RecvError> for Error {
    fn from(_err: tokio::sync::oneshot::error::RecvError) -> Self {
        Self::Internal(InternalError::RecvError)
    }
}

impl From<crossbeam::channel::RecvError> for Error {
    fn from(_err: crossbeam::channel::RecvError) -> Self {
        Self::Internal(InternalError::RecvError)
    }
}

impl<T> From<crossbeam::channel::SendError<T>> for Error {
    fn from(_err: crossbeam::channel::SendError<T>) -> Self {
        Self::Internal(InternalError::SendError)
    }
}

impl From<String> for Error {
    fn from(err: String) -> Self {
        Self::Other(err)
    }
}

impl From<std::io::Error> for Error {
    fn from(err: std::io::Error) -> Self {
        Self::Other(err.to_string())
    }
}

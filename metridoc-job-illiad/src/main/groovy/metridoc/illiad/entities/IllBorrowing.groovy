/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllBorrowing {

    public static final String AWAITING_REQUEST_PROCESSING = "Awaiting Request Processing"
    public static final String AWAITING_REQUEST_POST_PROCESSING = "Awaiting Post Receipt Processing"
    public static final String AWAITING_COPYRIGHT_CLEARANCE = "Awaiting Copyright Clearance"
    public static final String REQUEST_SENT = "Request Sent"
    public static final String SHIPPED = "Shipped"
    Long transactionNumber
    String requestType
    String transactionStatus
    Date transactionDate

    static mapping = {
        version(defaultValue: '0')
        transactionNumber(index: "idx_ill_borrowing_transaction_num")
        transactionStatus(index: "idx_ill_borrowing_transaction_num,idx_ill_borrowing_transaction_status")
    }

    static constraints = {
    }

}

/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

;(function($){
    "use strict";

    var jobIds = [];
    var timeout;

    $.fn.grabContent = function(opts) {
        jobIds = [];
        var options = $.extend({ }, opts);

        return this.each(function() {
            var $item = $(this);
            $item.find('#grabbit-form').submit(function(e){
                e.preventDefault();
                var paths = $('#grabbit-form #linkPaths').val().trim().split("\n");
                var queryURI = "/bin/twcable/client/grab?paths="+paths.join(",");
                try {
                    $(".grab-content").loadingbar({
                        async: true,
                        url : queryURI,
                        error: function(xhr, text, e) { $.fancybox({href: '#error'}); },
                        success: function(data, text, xhr) {
                            $.fancybox({href: '#success'});
                            jobIds = data;
                            grabStatus();
                        },
                        dataType: "json"
                    });
                } catch (e) {
                    console.log(e);
                }
            });
        });
    };

    function grabStatus() {
        timeout = setTimeout(doStatus, 5000);
    }


    //TODO : This is just stubbed out right now to show how 'status' can be reported. We can make it pretty or do it differently
    //once we know the proper usage
    function doStatus() {

            $.ajax({
                url: '/bin/twcable/client/grab/status?jobIds=' + jobIds.join(','),
                type: "GET",
                success: function(jobs) {
                    var done = 0;
                    $(".grab-status").html("");
                    for(var idx = 0; idx < jobs.length ; idx++) {
                        if(!jobs[idx].isRunning) {
                            done = done +1;
                        }

                        var status = "<span><u>Job Id</u> : " + jobs[idx].jobId +
                            "<br/><u>Path</u> : " + jobs[idx].path +
                            "<br/><u>Start Time</u> : " + jobs[idx].startTime +
                            "<br/><u>End Time</u> : " + jobs[idx].endTime +
                            "<br/><u>Done?</u> " + (!jobs[idx].isRunning ? "Yes" : "No") +
                            "<br/><u>ExitStatus</u> : " + jobs[idx].exitStatus.exitCode +
                            "<br/><u>Time Taken</u> : " + jobs[idx].timeTaken + " ms" +
                            "<br/><u># of Nodes</u> : " + jobs[idx].jcrNodesWritten +
                            "</span><hr><br/><br/>";
                        $(".grab-status").append(status);
                    }

                    if( done === jobs.length ) {
                        console.warn("Should end the timeout now")
                    }
                },
                dataType: "json",
                complete: doStatus
            })
        }

})(jQuery);

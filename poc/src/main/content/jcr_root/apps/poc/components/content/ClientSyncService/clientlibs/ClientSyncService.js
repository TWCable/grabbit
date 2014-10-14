;(function($){
    "use strict";

    var jobIds = [];
    var timeout;

    $.fn.grabContent = function(opts) {
        jobIds = [];
        var options = $.extend({ }, opts);

        return this.each(function() {
            var $item = $(this);
            $item.find('#sync-form').submit(function(e){
                e.preventDefault();
                var paths = $('#sync-form #linkPaths').val().trim().split("\n");
                var queryURI = "/bin/twc/client/grab?paths="+paths.join(",");
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
                url: '/bin/twc/client/grab/status?jobIds=' + jobIds.join(','),
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

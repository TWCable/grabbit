;(function($){
    "use strict";

    $.fn.replicateContent = function(opts) {
        var options = $.extend({ }, opts);

        return this.each(function() {
            var $item = $(this);
            $item.find('#sync-form').submit(function(e){
                e.preventDefault();
                var paths = $('#sync-form #linkPaths').val();
                var queryURI = "/bin/grabbit/client/pull?paths="+paths;
                try {
                    $(".grab-content").loadingbar({
                        async: true,
                        url : queryURI,
                        error: function(xhr, text, e) { $.fancybox({href: '#error'}); },
                        success: function(data, text, xhr) {  $.fancybox({href: '#success'}); },
                        dataType: "html"
                    });
                } catch (e) {}
            });
        });
    }
})(jQuery);

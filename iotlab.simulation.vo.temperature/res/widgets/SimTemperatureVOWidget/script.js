$(window).load(function() {
	w = new widget("update", 2000, function(data) {
		$("#loading").detach();
		$("p").show();
		$(".error").hide();
		if(data.error) {
			$("p").hide();
			$(".error").show();
			$(".error").text("No data received (yet)");			
		} else {
			$("p").show();
			$(".error").hide();
			$("#address").text(data.address);
			$("#temperature").text(data.temperature);
			$("#timestamp").text(data.timestamp);
		}
	});
	
	w.error = function(msg) {
		$("#loading").detach();
		$("p").hide();
		$(".error").show();
		$(".error").text(msg);
	}
});
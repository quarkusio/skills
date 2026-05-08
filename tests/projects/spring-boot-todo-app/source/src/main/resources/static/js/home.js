$(document).ready(function () {
  // Handle form submission
  $("#taskForm").submit(function (event) {
    event.preventDefault(); // Prevent the default form submission

    // Get form data
    let title = $("#title").val();
    let description = $("#description").val();
    let dueDate = $("#dueDate").val();

    let csrfToken = $("meta[name='_csrf']").attr("content");
    let csrfHeader = $("meta[name='_csrf_header']").attr("content");

    // Send a POST request to your server
    $.ajax({
      type: "POST",
      url: "/home", // Replace with your server endpoint
      contentType: "application/json; charset=utf-8",
      data: JSON.stringify({ 'title': title, 'description': description, 'dueDate': dueDate }),
      headers: {
        [csrfHeader]: csrfToken
      },
      success: function (response) {
        $.ajax({
          type: "GET",
          url: window.location.href,
          contentType: "text/html; charset=UTF-8",
          headers: {
            [csrfHeader]: csrfToken
          },
          success: function (response) {
            confirm("Task created successfully!");
            $("body").html(response);
          },
          error: function (error) {
            console.error("Error getting tasks: " + error.responseText);
          }
        })
      },
      error: function (error) {
        console.error("Error creating task: " + error.responseText);
      }
    });

    // Clear form fields
    $("#title").val("");
    $("#description").val("");
    $("#dueDate").val("");
  });
});
package javastrava.api.v3.rest;

import java.time.LocalDateTime;

import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaActivityUpdate;
import javastrava.api.v3.model.StravaActivityZone;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaComment;
import javastrava.api.v3.model.StravaLap;
import javastrava.api.v3.model.StravaPhoto;
import javastrava.api.v3.model.StravaResponse;
import javastrava.api.v3.rest.async.StravaAPICallback;
import javastrava.api.v3.service.ActivityService;
import javastrava.api.v3.service.exception.BadRequestException;
import javastrava.api.v3.service.exception.NotFoundException;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**<p>
 * API declarations of activity service endpoints
 * </p>
 *
 * @author Dan Shannon
 *
 */
public interface ActivityAPI {

	/**
	 * @see ActivityService#createComment(Integer, String)
	 *
	 * @param activityId Activity identifier
	 * @param text Text of the comment to create
	 * @return The comment as posted
	 * @throws NotFoundException If the activity does not exist
	 * @throws BadRequestException If the comment text is null or the empty string
	 */
	@POST("/activities/{id}/comments")
	public StravaComment createComment(@Path("id") final Long activityId, @Query("text") final String text) throws BadRequestException, NotFoundException;

	/**
	 * @see ActivityService#createComment(Integer, String)
	 *
	 * @param activityId Activity identifier
	 * @param text Text of the comment to create
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity does not exist
	 * @throws BadRequestException If the comment text is null or the empty string
	 */
	@POST("/activities/{id}/comments")
	public void createComment(@Path("id") final Long activityId, @Query("text") final String text, final StravaAPICallback<StravaComment> callback) throws BadRequestException, NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#createManualActivity(javastrava.api.v3.model.StravaActivity)
	 *
	 * @param activity The activity to be created on Strava
	 * @return The activity as stored by Strava
	 * @throws BadRequestException If the activity is malformed and can't be uploaded
	 */
	@POST("/activities")
	public StravaActivity createManualActivity(@Body final StravaActivity activity) throws BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#createManualActivity(javastrava.api.v3.model.StravaActivity)
	 *
	 * @param activity The activity to be created on Strava
	 * @param callback Callback to be executed once the call is completed
	 * @throws BadRequestException If the activity is malformed and can't be uploaded
	 */
	@POST("/activities")
	public void createManualActivity(@Body final StravaActivity activity, final StravaAPICallback<StravaActivity> callback) throws BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteActivity(java.lang.Integer)
	 *
	 * @param id Activity identifier
	 * @return Activity that has been successfully deleted from Strava
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@DELETE("/activities/{id}")
	public StravaActivity deleteActivity(@Path("id") final Long id) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteActivity(java.lang.Integer)
	 *
	 * @param id Activity identifier
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@DELETE("/activities/{id}")
	public void deleteActivity(@Path("id") final Long id, final StravaAPICallback<StravaActivity> callback) throws NotFoundException;

	/**
	 * @see ActivityService#deleteComment(Integer, Integer)
	 *
	 * @param activityId Id of the activity the comment was posted to
	 * @param commentId Id of the comment
	 * @return Strava response
	 * @throws NotFoundException If the comment does not exist
	 */
	@DELETE("/activities/{activityId}/comments/{commentId}")
	public StravaResponse deleteComment(@Path("activityId") final Long activityId, @Path("commentId") final Integer commentId) throws NotFoundException;

	/**
	 * @see ActivityService#deleteComment(Integer, Integer)
	 *
	 * @param activityId Id of the activity the comment was posted to
	 * @param commentId Id of the comment
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the comment does not exist
	 */
	@DELETE("/activities/{activityId}/comments/{commentId}")
	public void deleteComment(@Path("activityId") final Long activityId, @Path("commentId") final Integer commentId, final StravaAPICallback<StravaResponse> callback) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#getActivity(java.lang.Integer, java.lang.Boolean)
	 */
	/**
	 * @param id
	 *            The id of the {@link StravaActivity activity} to be returned
	 * @param includeAllEfforts
	 *            (Optional) Used to include all segment efforts in the result (if omitted or <code>false</code> then only
	 *            "important" efforts are returned).
	 * @return Returns a detailed representation if the {@link StravaActivity activity} is owned by the requesting athlete. Returns
	 *         a summary representation for all other requests.
	 * @throws NotFoundException If the activity does not exist
	 */
	@GET("/activities/{id}")
	public StravaActivity getActivity(@Path("id") final Long id, @Query("include_all_efforts") final Boolean includeAllEfforts) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#getActivity(java.lang.Integer, java.lang.Boolean)
	 *
	 * @param id
	 *            The id of the {@link StravaActivity activity} to be returned
	 * @param includeAllEfforts
	 *            (Optional) Used to include all segment efforts in the result (if omitted or <code>false</code> then only
	 *            "important" efforts are returned).
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity does not exist
	 */
	@GET("/activities/{id}")
	public void getActivity(@Path("id") final Long id, @Query("include_all_efforts") final Boolean includeAllEfforts, final StravaAPICallback<StravaActivity> callback) throws NotFoundException;

	/**
	 * @see ActivityService#giveKudos(Integer)
	 *
	 * @param activityId Activity to be kudoed
	 * @return Strava response
	 * @throws NotFoundException if the activity does not exist
	 */
	@POST("/activities/{id}/kudos")
	public StravaResponse giveKudos(@Path("id") final Long activityId) throws NotFoundException;

	/**
	 * @see ActivityService#giveKudos(Integer)
	 *
	 * @param activityId Activity to be kudoed
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException if the activity does not exist
	 */
	@POST("/activities/{id}/kudos")
	public void giveKudos(@Path("id") final Long activityId, final StravaAPICallback<StravaResponse> callback) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityComments(Integer, Boolean, javastrava.util.Paging)
	 *
	 * @param activityId Activity identifier
	 * @param markdown Whether or not to return comments including markdown
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @return Array of comments belonging to the activity
	 * @throws NotFoundException If the activity doesn't exist
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/{id}/comments")
	public StravaComment[] listActivityComments(@Path("id") final Long activityId, @Query("markdown") final Boolean markdown, @Query("page") final Integer page,
			@Query("per_page") final Integer perPage) throws NotFoundException, BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityComments(Integer, Boolean, javastrava.util.Paging)
	 *
	 * @param activityId Activity identifier
	 * @param markdown Whether or not to return comments including markdown
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/{id}/comments")
	public void listActivityComments(@Path("id") final Long activityId, @Query("markdown") final Boolean markdown, @Query("page") final Integer page,
			@Query("per_page") final Integer perPage, final StravaAPICallback<StravaComment[]> callback) throws NotFoundException, BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityKudoers(Integer, javastrava.util.Paging)
	 *
	 * @param activityId Activity identifier
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @return Array of athletes who have kudoed the activity
	 * @throws NotFoundException If the activity doesn't exist
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/{id}/kudos")
	public StravaAthlete[] listActivityKudoers(@Path("id") final Long activityId, @Query("page") final Integer page, @Query("per_page") final Integer perPage)
			throws NotFoundException, BadRequestException;


	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityKudoers(Integer, javastrava.util.Paging)
	 *
	 * @param activityId Activity identifier
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/{id}/kudos")
	public void listActivityKudoers(@Path("id") final Long activityId, @Query("page") final Integer page, @Query("per_page") final Integer perPage, final StravaAPICallback<StravaAthlete[]> callback)
			throws NotFoundException, BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityLaps(java.lang.Integer)
	 *
	 * @param activityId The activity identifier
	 * @return Array of laps belonging to the activity
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@GET("/activities/{id}/laps")
	public StravaLap[] listActivityLaps(@Path("id") final Long activityId) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityLaps(java.lang.Integer)
	 *
	 * @param activityId The activity identifier
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@GET("/activities/{id}/laps")
	public void listActivityLaps(@Path("id") final Long activityId, final StravaAPICallback<StravaLap[]> callback) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityPhotos(java.lang.Integer)
	 *
	 * @param activityId Activity identifier
	 * @return Array of photos attached to the activity
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@GET("/activities/{id}/photos")
	public StravaPhoto[] listActivityPhotos(@Path("id") final Long activityId) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityPhotos(java.lang.Integer)
	 *
	 * @param activityId Activity identifier
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@GET("/activities/{id}/photos")
	public void listActivityPhotos(@Path("id") final Long activityId, final StravaAPICallback<StravaPhoto[]> callback) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityZones(java.lang.Integer)
	 *
	 * @param activityId The activity identifier
	 * @return Array of activity zones for the activity
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@GET("/activities/{id}/zones")
	public StravaActivityZone[] listActivityZones(@Path("id") final Long activityId) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityZones(java.lang.Integer)
	 *
	 * @param activityId The activity identifier
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@GET("/activities/{id}/zones")
	public void listActivityZones(@Path("id") final Long activityId, final StravaAPICallback<StravaActivityZone[]> callback) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivities(LocalDateTime, LocalDateTime, javastrava.util.Paging)
	 *
	 * @param before Unix epoch time in seconds - return activities before this time
	 * @param after Unix epoch time in seconds - return activities after this time
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @return List of Strava activities in the given time frame
	 * @throws BadRequestException If paging instructions are invalid
	 */
	@GET("/athlete/activities")
	public StravaActivity[] listAuthenticatedAthleteActivities(@Query("before") final Integer before, @Query("after") final Integer after, @Query("page") final Integer page,
			@Query("per_page") final Integer perPage) throws BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivities(LocalDateTime, LocalDateTime, javastrava.util.Paging)
	 *
	 * @param before Unix epoch time in seconds - return activities before this time
	 * @param after Unix epoch time in seconds - return activities after this time
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @param callback Callback to be executed once the call is completed
	 * @throws BadRequestException If paging instructions are invalid
	 */
	@GET("/athlete/activities")
	public void listAuthenticatedAthleteActivities(@Query("before") final Integer before, @Query("after") final Integer after, @Query("page") final Integer page,
			@Query("per_page") final Integer perPage, final StravaAPICallback<StravaActivity[]> callback) throws BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listFriendsActivities(javastrava.util.Paging)
	 *
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @return List of Strava activities belonging to friends of the authenticated athlete
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/following")
	public StravaActivity[] listFriendsActivities(@Query("page") final Integer page, @Query("per_page") final Integer perPage) throws BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listFriendsActivities(javastrava.util.Paging)
	 *
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @param callback Callback to be executed once the call is completed
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/following")
	public void listFriendsActivities(@Query("page") final Integer page, @Query("per_page") final Integer perPage, final StravaAPICallback<StravaActivity[]> callback) throws BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listRelatedActivities(java.lang.Integer, javastrava.util.Paging)
	 *
	 * @param activityId Activity identifier
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @return Array of activities that Strava judges was 'done with' the activity identified by the id
	 * @throws NotFoundException If the activity doesn't exist
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/{id}/related")
	public StravaActivity[] listRelatedActivities(@Path("id") final Long activityId, @Query("page") final Integer page, @Query("per_page") final Integer perPage)
			throws NotFoundException, BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#listRelatedActivities(java.lang.Integer, javastrava.util.Paging)
	 *
	 * @param activityId Activity identifier
	 * @param page Page number to be returned
	 * @param perPage Page size to be returned
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 * @throws BadRequestException If the paging instructions are invalid
	 */
	@GET("/activities/{id}/related")
	public void listRelatedActivities(@Path("id") final Long activityId, @Query("page") final Integer page, @Query("per_page") final Integer perPage, final StravaAPICallback<StravaActivity[]> callback)
			throws NotFoundException, BadRequestException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#updateActivity(Integer, StravaActivityUpdate)
	 *
	 * @param id The id of the activity to update
	 * @param activity The activity details
	 * @return The activity as stored on Strava
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@PUT("/activities/{id}")
	public StravaActivity updateActivity(@Path("id") final Long id, @Body final StravaActivityUpdate activity) throws NotFoundException;

	/**
	 * @see javastrava.api.v3.service.ActivityService#updateActivity(Integer, StravaActivityUpdate)
	 *
	 * @param id The id of the activity to update
	 * @param activity The activity details
	 * @param callback Callback to be executed once the call is completed
	 * @throws NotFoundException If the activity doesn't exist
	 */
	@PUT("/activities/{id}")
	public void updateActivity(@Path("id") final Long id, @Body final StravaActivityUpdate activity, final StravaAPICallback<StravaActivity> callback) throws NotFoundException;
}

package javastrava.api.v3.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaActivityUpdate;
import javastrava.api.v3.model.StravaActivityZone;
import javastrava.api.v3.model.StravaAthlete;
import javastrava.api.v3.model.StravaComment;
import javastrava.api.v3.model.StravaLap;
import javastrava.api.v3.model.StravaPhoto;
import javastrava.api.v3.model.reference.StravaResourceState;
import javastrava.api.v3.service.ActivityService;
import javastrava.api.v3.service.exception.BadRequestException;
import javastrava.api.v3.service.exception.NotFoundException;
import javastrava.api.v3.service.exception.StravaInternalServerErrorException;
import javastrava.api.v3.service.exception.StravaUnknownAPIException;
import javastrava.api.v3.service.exception.UnauthorizedException;
import javastrava.cache.StravaCache;
import javastrava.cache.impl.StravaCacheImpl;
import javastrava.config.Messages;
import javastrava.util.Paging;
import javastrava.util.PagingHandler;
import javastrava.util.PrivacyUtils;
import javastrava.util.StravaDateUtils;

/**
 * @author Dan Shannon
 *
 */
public class ActivityServiceImpl extends StravaServiceImpl implements ActivityService {
	/**
	 * <p>
	 * Returns an instance of {@link ActivityService activity services}
	 * </p>
	 *
	 * <p>
	 * Instances are cached so that if 2 requests are made for the same token,
	 * the same instance is returned
	 * </p>
	 *
	 * @param token
	 *            The Strava access token to be used in requests to the Strava
	 *            API
	 * @return An instance of the activity services
	 */
	public static ActivityService instance(final Token token) {
		// Get the service from the token's cache
		ActivityService service = token.getService(ActivityService.class);

		// If it's not already there, create a new one and put it in the token
		if (service == null) {
			service = new ActivityServiceImpl(token);
			token.addService(ActivityService.class, service);
		}
		return service;
	}

	/**
	 * Cache of activities
	 */
	private final StravaCache<StravaActivity, Long> activityCache;

	/**
	 * Cache of comments
	 */
	private final StravaCache<StravaComment, Integer> commentCache;

	/**
	 * Cache of laps
	 */
	private final StravaCache<StravaLap, Integer> lapCache;

	/**
	 * Cache of photos
	 */
	private final StravaCache<StravaPhoto, Integer> photoCache;

	/**
	 * <p>
	 * Private constructor requires a valid access token
	 * </p>
	 *
	 * @param token Access token from Strava OAuth process
	 */
	private ActivityServiceImpl(final Token token) {
		super(token);
		this.activityCache = new StravaCacheImpl<StravaActivity, Long>(StravaActivity.class, token);
		this.commentCache = new StravaCacheImpl<StravaComment, Integer>(StravaComment.class, token);
		this.lapCache = new StravaCacheImpl<StravaLap, Integer>(StravaLap.class, token);
		this.photoCache = new StravaCacheImpl<StravaPhoto, Integer>(StravaPhoto.class, token);
	}

	/**
	 * @see javastrava.api.v3.service.StravaService#clearCache()
	 */
	@Override
	public void clearCache() {
		this.activityCache.removeAll();
		this.commentCache.removeAll();
		this.lapCache.removeAll();
		this.photoCache.removeAll();
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#createComment(java.lang.Integer,
	 *      java.lang.String)
	 */
	@Override
	public StravaComment createComment(final Long activityId, final String text) throws NotFoundException,
	BadRequestException {
		//		if ((text == null) || text.equals("")) { //$NON-NLS-1$
		//			throw new IllegalArgumentException(Messages.string("ActivityServiceImpl.commentCannotBeEmpty")); //$NON-NLS-1$
		//		}

		// TODO This is a workaround for issue #30 - API allows comments to be posted without write access
		// Token must have write access
		if (!(getToken().hasWriteAccess())) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.commentWithoutWriteAccess")); //$NON-NLS-1$
		}
		// End of workaround

		// Activity must exist
		final StravaActivity activity = getActivity(activityId);
		if (activity == null) {
			throw new NotFoundException(Messages.string("ActivityServiceImpl.commentOnInvalidActivity")); //$NON-NLS-1$
		}

		// If activity is private and not accessible, cannot be commented on
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.commentOnPrivateActivity")); //$NON-NLS-1$
		}

		// Create the comment
		final StravaComment comment = this.api.createComment(activityId, text);

		// Put the comment in cache
		this.commentCache.put(comment);

		// Return the comment
		return comment;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#createCommentAsync(java.lang.Integer, java.lang.String)
	 */
	@Override
	public CompletableFuture<StravaComment> createCommentAsync(final Long activityId, final String text) throws NotFoundException, BadRequestException {
		return StravaServiceImpl.future(() -> {
			return createComment(activityId, text);
		});

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#createManualActivity(javastrava.api.v3.model.StravaActivity)
	 */
	@Override
	public StravaActivity createManualActivity(final StravaActivity activity) {
		// Token must have write access
		if (!getToken().hasWriteAccess()) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.createActivityWithoutWriteAccess")); //$NON-NLS-1$
		}

		// Token must have view_private to write a private activity
		if ((activity.getPrivateActivity() != null) && activity.getPrivateActivity().equals(Boolean.TRUE)
				&& !getToken().hasViewPrivate()) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.createPrivateActivityWithoutViewPrivate")); //$NON-NLS-1$
		}

		// Create the activity
		StravaActivity stravaResponse = null;
		try {
			stravaResponse = this.api.createManualActivity(activity);
		} catch (final BadRequestException e) {
			throw new IllegalArgumentException(e);
		}
		// TODO Workaround for issue javastrava-api #49
		// (https://github.com/danshannon/javastravav3api/issues/49)
		catch (final StravaInternalServerErrorException e) {
			throw new IllegalArgumentException(e);
		}
		// End of workaround

		// Put the activity in cache
		if (stravaResponse.getResourceState() != StravaResourceState.UPDATING) {
			this.activityCache.put(stravaResponse);
		}

		// Return the activity
		return stravaResponse;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#createManualActivityAsync(javastrava.api.v3.model.StravaActivity)
	 */
	@Override
	public CompletableFuture<StravaActivity> createManualActivityAsync(final StravaActivity activity) {
		return StravaServiceImpl.future(() -> {
			return createManualActivity(activity);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteActivity(java.lang.Integer)
	 */
	@Override
	public StravaActivity deleteActivity(final Long id) throws NotFoundException {
		// Token must have write access
		if (!getToken().hasWriteAccess()) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.deleteActivityWithoutWriteAccess")); //$NON-NLS-1$
		}

		// Activity must exist
		StravaActivity activity = getActivity(id);
		if (activity == null) {
			throw new NotFoundException(Messages.string("ActivityServiceImpl.deleteInvalidActivity")); //$NON-NLS-1$
		}

		// To delete a private activity, token must have view_private access
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.deletePrivateActivityWithoutViewPrivate")); //$NON-NLS-1$
		}

		// Now we can do the delete
		try {
			activity = this.api.deleteActivity(id);
		} catch (final NotFoundException e) {
			return null;
		}

		// If the delete worked, also remove it from the cache
		this.activityCache.remove(id);

		// And finally, return it
		return activity;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteActivityAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<StravaActivity> deleteActivityAsync(final Long activityId) throws NotFoundException {
		return StravaServiceImpl.future(() -> {
			return deleteActivity(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteComment(java.lang.Integer,
	 *      java.lang.Integer)
	 */
	@Override
	public void deleteComment(final Long activityId, final Integer commentId) throws NotFoundException {
		// TODO This is a workaround for issue #63 (can delete comments without write access)
		// Token must have write access
		if (!(getToken().hasWriteAccess())) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.deleteCommentWithoutWriteAccess")); //$NON-NLS-1$
		}
		// End of workaround

		// Activity must exist
		final StravaActivity activity = getActivity(activityId);
		if (activity == null) {
			throw new NotFoundException(Messages.string("ActivityServiceImpl.deleteCommentOnInvalidActivity")); //$NON-NLS-1$
		}

		// Token must have view_private to delete a comment on a private
		// activity
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.deleteCommentOnPrivateActivity")); //$NON-NLS-1$
		}
		// End of workaround

		// Delete the comment
		this.api.deleteComment(activityId, commentId);

		// Remove it from the cache
		this.commentCache.remove(commentId);

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteComment(javastrava.api.v3.model.StravaComment)
	 */
	@Override
	public void deleteComment(final StravaComment comment) throws NotFoundException {
		deleteComment(comment.getActivityId(), comment.getId());
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteCommentAsync(java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public CompletableFuture<Void> deleteCommentAsync(final Long activityId, final Integer commentId) throws NotFoundException {
		return StravaServiceImpl.future(() -> {
			deleteComment(activityId, commentId);
			return null;
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#deleteCommentAsync(javastrava.api.v3.model.StravaComment)
	 */
	@Override
	public CompletableFuture<Void> deleteCommentAsync(final StravaComment comment) throws NotFoundException {
		return StravaServiceImpl.future(() -> {
			deleteComment(comment);
			return null;
		});
	}

	/**
	 * Update the given activity
	 * @param id Activity identifier
	 * @param update Updates to be made to the activity
	 * @return Activity returned from Strava as a result of the update
	 */
	private StravaActivity doUpdateActivity(final Long id, final StravaActivityUpdate update) {
		try {
			this.activityCache.remove(id);
			final StravaActivity response = this.api.updateActivity(id, update);
			return response;
		} catch (final NotFoundException e) {
			return null;
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#getActivity(java.lang.Integer)
	 */
	@Override
	public StravaActivity getActivity(final Long id) {
		return getActivity(id, Boolean.FALSE);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#getActivity(java.lang.Integer,
	 *      java.lang.Boolean)
	 */
	@Override
	public StravaActivity getActivity(final Long activityId, final Boolean includeAllEfforts) {
		// Attempt to get the activity from cache
		StravaActivity stravaResponse = this.activityCache.get(activityId);
		if ((stravaResponse != null) && (stravaResponse.getResourceState() != StravaResourceState.META)) {
			return stravaResponse;
		}

		// If it wasn't in cache, then get it from the API
		try {
			stravaResponse = this.api.getActivity(activityId, includeAllEfforts);
		} catch (final NotFoundException e) {
			// Activity doesn't exist - return null
			return null;
		} catch (final UnauthorizedException e) {
			stravaResponse = PrivacyUtils.privateActivity(activityId);
		}

		// Put the activity in cache unless it's UPDATING
		if (stravaResponse.getResourceState() != StravaResourceState.UPDATING) {
			this.activityCache.put(stravaResponse);
		}

		// And return it
		return stravaResponse;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#getActivityAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<StravaActivity> getActivityAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return getActivity(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#getActivityAsync(java.lang.Integer, java.lang.Boolean)
	 */
	@Override
	public CompletableFuture<StravaActivity> getActivityAsync(final Long activityId, final Boolean includeAllEfforts) {
		return StravaServiceImpl.future(() -> {
			return getActivity(activityId, includeAllEfforts);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#giveKudos(java.lang.Integer)
	 */
	@Override
	public void giveKudos(final Long activityId) throws NotFoundException {
		// Must have write access to give kudos
		if (!(getToken().hasWriteAccess())) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.kudosWithoutWriteAccess")); //$NON-NLS-1$
		}

		// Activity must exist
		final StravaActivity activity = getActivity(activityId);
		if (activity == null) {
			throw new NotFoundException(Messages.string("ActivityServiceImpl.kudosInvalidActivity")); //$NON-NLS-1$
		}

		// Must have view_private to give kudos to a private activity
		if (!getToken().hasViewPrivate() && (activity.getResourceState() == StravaResourceState.PRIVATE)) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.kudosPrivateActivityWithoutViewPrivate")); //$NON-NLS-1$
		}

		this.api.giveKudos(activityId);

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#giveKudosAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<Void> giveKudosAsync(final Long activityId) throws NotFoundException {
		return StravaServiceImpl.future(() -> {
			giveKudos(activityId);
			return null;
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityComments(java.lang.Integer)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Long id) {
		return listActivityComments(id, Boolean.FALSE);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityComments(java.lang.Integer,
	 *      java.lang.Boolean)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Long id, final Boolean markdown) {
		return listActivityComments(id, markdown, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityComments(Integer,
	 *      Boolean, Paging)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Long id, final Boolean markdown,
			final Paging pagingInstruction) {
		// If the activity doesn't exist, then neither do the comments
		final StravaActivity activity = getActivity(id);
		if (activity == null) { // doesn't exist
			return null;
		}

		// If the activity is private and not accessible, don't return the
		// comments
		if (activity.getResourceState() == StravaResourceState.PRIVATE) { // is
			// private
			return new ArrayList<StravaComment>();
		}

		// Get the comments from Strava
		final List<StravaComment> comments = PagingHandler.handlePaging(
				pagingInstruction,
				thisPage -> Arrays.asList(ActivityServiceImpl.this.api.listActivityComments(id, markdown,
						thisPage.getPage(), thisPage.getPageSize())));

		// And put them in the cache
		this.commentCache.putAll(comments);

		// Finally, return the list
		return comments;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityComments(java.lang.Integer,
	 *      javastrava.util.Paging)
	 */
	@Override
	public List<StravaComment> listActivityComments(final Long id, final Paging pagingInstruction) {
		return listActivityComments(id, Boolean.FALSE, pagingInstruction);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityCommentsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaComment>> listActivityCommentsAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listActivityComments(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityCommentsAsync(java.lang.Integer, java.lang.Boolean)
	 */
	@Override
	public CompletableFuture<List<StravaComment>> listActivityCommentsAsync(final Long activityId, final Boolean markdown) {
		return StravaServiceImpl.future(() -> {
			return listActivityComments(activityId, markdown);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityCommentsAsync(java.lang.Integer, java.lang.Boolean, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaComment>> listActivityCommentsAsync(final Long activityId, final Boolean markdown, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listActivityComments(activityId, markdown, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityCommentsAsync(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaComment>> listActivityCommentsAsync(final Long activityId, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listActivityComments(activityId, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityKudoers(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listActivityKudoers(final Long id) {
		return listActivityKudoers(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityKudoers(Integer,
	 *      Paging)
	 */
	@Override
	public List<StravaAthlete> listActivityKudoers(final Long id, final Paging pagingInstruction) {
		// If the activity doesn't exist, then neither do the kudoers
		final StravaActivity activity = getActivity(id);
		if (activity == null) {
			return null;
		}

		// If the activity is private and inaccessible, return an empty list
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			return new ArrayList<StravaAthlete>();
		}

		return PagingHandler.handlePaging(pagingInstruction, thisPage -> Arrays.asList(ActivityServiceImpl.this.api
				.listActivityKudoers(id, thisPage.getPage(), thisPage.getPageSize())));

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityKudoersAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listActivityKudoersAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listActivityKudoers(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityKudoersAsync(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listActivityKudoersAsync(final Long activityId, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listActivityKudoers(activityId, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityLaps(java.lang.Integer)
	 */
	@Override
	public List<StravaLap> listActivityLaps(final Long id) {
		// If the activity doesn't exist, return null
		final StravaActivity activity = getActivity(id);
		if (activity == null) {
			return null;
		}

		// If the activity is private and inaccessible, return an empty list
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			return new ArrayList<StravaLap>();
		}

		// Try to get the laps from cache
		final List<StravaLap> cachedLaps = this.lapCache.list();
		List<StravaLap> laps = new ArrayList<StravaLap>();
		for (final StravaLap lap : cachedLaps) {
			if (lap.getActivity().getId().equals(id)) {
				laps.add(lap);
			}
		}
		if (!laps.isEmpty()) {
			return laps;
		}

		// Get the laps from Strava
		try {
			laps = Arrays.asList(this.api.listActivityLaps(id));
		} catch (final NotFoundException e) {
			return null;
		}

		// Put them all in the cache
		this.lapCache.putAll(laps);

		// Finally, return the laps
		return laps;

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityLapsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaLap>> listActivityLapsAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listActivityLaps(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityPhotos(java.lang.Integer)
	 */
	@Override
	public List<StravaPhoto> listActivityPhotos(final Long id) {
		// If the activity doesn't exist, return null
		final StravaActivity activity = getActivity(id);
		if (activity == null) {
			return null;
		}

		// If the activity is private and inaccessible, return an empty list
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			return new ArrayList<StravaPhoto>();
		}

		// Try to get the photos from cache
		final List<StravaPhoto> cachedPhotos = this.photoCache.list();
		List<StravaPhoto> photos = new ArrayList<StravaPhoto>();
		for (final StravaPhoto photo : cachedPhotos) {
			if (photo.getActivityId().equals(id)) {
				photos.add(photo);
			}
		}

		// Attempt to get the photos from Strava
		try {
			final StravaPhoto[] photoArray = this.api.listActivityPhotos(id);

			photos = Arrays.asList(photoArray);

		} catch (final NotFoundException e) {
			return null;
		}

		// Put all the photos in cache
		this.photoCache.putAll(photos);

		// Return the photos
		return photos;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityPhotosAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaPhoto>> listActivityPhotosAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listActivityPhotos(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityZones(java.lang.Integer)
	 */
	@Override
	public List<StravaActivityZone> listActivityZones(final Long id) {
		// If the activity doesn't exist, return null
		final StravaActivity activity = getActivity(id);
		if (activity == null) {
			return null;
		}

		// If the activity is private and inaccesible, return an empty list
		if (activity.getResourceState() == StravaResourceState.PRIVATE) {
			return new ArrayList<StravaActivityZone>();
		}

		try {
			return Arrays.asList(this.api.listActivityZones(id));
		} catch (final NotFoundException e) {
			return null;
		}
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listActivityZonesAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaActivityZone>> listActivityZonesAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listActivityZones(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllActivityComments(java.lang.Integer)
	 */
	@Override
	public List<StravaComment> listAllActivityComments(final Long activityId) {
		return PagingHandler.handleListAll(thisPage -> listActivityComments(activityId, thisPage));
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllActivityCommentsAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaComment>> listAllActivityCommentsAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listAllActivityComments(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllActivityKudoers(java.lang.Integer)
	 */
	@Override
	public List<StravaAthlete> listAllActivityKudoers(final Long activityId) {
		return PagingHandler.handleListAll(thisPage -> listActivityKudoers(activityId, thisPage));
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllActivityKudoersAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaAthlete>> listAllActivityKudoersAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listAllActivityKudoers(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllAuthenticatedAthleteActivities()
	 */
	@Override
	public List<StravaActivity> listAllAuthenticatedAthleteActivities() {
		return PagingHandler.handleListAll(thisPage -> listAuthenticatedAthleteActivities(thisPage));

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllAuthenticatedAthleteActivities(LocalDateTime,
	 *      LocalDateTime)
	 */
	@Override
	public List<StravaActivity> listAllAuthenticatedAthleteActivities(final LocalDateTime before,
			final LocalDateTime after) {
		final List<StravaActivity> activities = PagingHandler
				.handleListAll(thisPage -> listAuthenticatedAthleteActivities(before, after, thisPage));

		return activities;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllAuthenticatedAthleteActivitiesAsync()
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAllAuthenticatedAthleteActivitiesAsync() {
		return StravaServiceImpl.future(() -> {
			return listAllAuthenticatedAthleteActivities();
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllAuthenticatedAthleteActivitiesAsync(java.time.LocalDateTime, java.time.LocalDateTime)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAllAuthenticatedAthleteActivitiesAsync(final LocalDateTime before, final LocalDateTime after) {
		return StravaServiceImpl.future(() -> {
			return listAllAuthenticatedAthleteActivities(before, after);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllFriendsActivities()
	 */
	@Override
	public List<StravaActivity> listAllFriendsActivities() {
		return PagingHandler.handleListAll(thisPage -> listFriendsActivities(thisPage));
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllFriendsActivitiesAsync()
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAllFriendsActivitiesAsync() {
		return StravaServiceImpl.future(() -> {
			return listAllFriendsActivities();
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllRelatedActivities(java.lang.Integer)
	 */
	@Override
	public List<StravaActivity> listAllRelatedActivities(final Long activityId) {
		return PagingHandler.handleListAll(thisPage -> listRelatedActivities(activityId, thisPage));
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAllRelatedActivitiesAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAllRelatedActivitiesAsync(final Long activityId) {
		return StravaServiceImpl.future(() -> {
			return listAllRelatedActivities(activityId);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivities()
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities() {
		return listAuthenticatedAthleteActivities(null, null, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivities(LocalDateTime,
	 *      LocalDateTime)
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities(final LocalDateTime before, final LocalDateTime after) {
		return listAuthenticatedAthleteActivities(before, after, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivities(LocalDateTime,
	 *      LocalDateTime, Paging)
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities(final LocalDateTime before,
			final LocalDateTime after, final Paging pagingInstruction) {
		final Integer secondsBefore = StravaDateUtils.secondsSinceUnixEpoch(before);
		final Integer secondsAfter = StravaDateUtils.secondsSinceUnixEpoch(after);

		// Get the activities from Strava
		List<StravaActivity> activities = PagingHandler.handlePaging(pagingInstruction, thisPage -> Arrays
				.asList(this.api.listAuthenticatedAthleteActivities(secondsBefore, secondsAfter, thisPage.getPage(),
						thisPage.getPageSize())));

		// Handle Strava's slight weirdnesses with privacy
		activities = PrivacyUtils.handlePrivateActivities(activities, this.getToken());

		// Put the activities in the cache
		this.activityCache.putAll(activities);

		// Return them
		return activities;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivities(javastrava.util.Paging)
	 */
	@Override
	public List<StravaActivity> listAuthenticatedAthleteActivities(final Paging pagingInstruction) {
		return listAuthenticatedAthleteActivities(null, null, pagingInstruction);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivitiesAsync()
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAuthenticatedAthleteActivitiesAsync() {
		return StravaServiceImpl.future(() -> {
			return listAuthenticatedAthleteActivities();
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivitiesAsync(java.time.LocalDateTime, java.time.LocalDateTime)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAuthenticatedAthleteActivitiesAsync(final LocalDateTime before, final LocalDateTime after) {
		return StravaServiceImpl.future(() -> {
			return listAuthenticatedAthleteActivities(before, after);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivitiesAsync(java.time.LocalDateTime, java.time.LocalDateTime, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAuthenticatedAthleteActivitiesAsync(final LocalDateTime before, final LocalDateTime after,
			final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listAuthenticatedAthleteActivities(before, after, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listAuthenticatedAthleteActivitiesAsync(javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listAuthenticatedAthleteActivitiesAsync(final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listAuthenticatedAthleteActivities(pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listFriendsActivities()
	 */
	@Override
	public List<StravaActivity> listFriendsActivities() {
		return listFriendsActivities(null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listFriendsActivities(Paging)
	 */
	@Override
	public List<StravaActivity> listFriendsActivities(final Paging pagingInstruction) {
		// Attempt to get the activities from Strava
		List<StravaActivity> activities = PagingHandler.handlePaging(pagingInstruction,
				thisPage -> Arrays.asList(this.api.listFriendsActivities(thisPage.getPage(), thisPage.getPageSize())));

		// Handle any privacy errors
		activities = PrivacyUtils.handlePrivateActivities(activities, this.getToken());

		// Put the activities in the cache
		this.activityCache.putAll(activities);

		// Return the activities
		return activities;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listFriendsActivitiesAsync()
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listFriendsActivitiesAsync() {
		return StravaServiceImpl.future(() -> {
			return listFriendsActivities();
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listFriendsActivitiesAsync(javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listFriendsActivitiesAsync(final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listFriendsActivities(pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listRelatedActivities(java.lang.Integer)
	 */
	@Override
	public List<StravaActivity> listRelatedActivities(final Long id) {
		return listRelatedActivities(id, null);
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listRelatedActivities(java.lang.Integer,
	 *      javastrava.util.Paging)
	 */
	@Override
	public List<StravaActivity> listRelatedActivities(final Long id, final Paging pagingInstruction) {
		// Attempt to get the activities from Strava
		List<StravaActivity> activities = PagingHandler.handlePaging(pagingInstruction, thisPage -> Arrays
				.asList(ActivityServiceImpl.this.api.listRelatedActivities(id, thisPage.getPage(),
						thisPage.getPageSize())));

		// Handle any privacy errors
		activities = PrivacyUtils.handlePrivateActivities(activities, this.getToken());

		// Put all the activities in cache
		this.activityCache.putAll(activities);

		// Return the activities
		return activities;
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listRelatedActivitiesAsync(java.lang.Integer)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listRelatedActivitiesAsync(final Long id) {
		return StravaServiceImpl.future(() -> {
			return listRelatedActivities(id);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#listRelatedActivitiesAsync(java.lang.Integer, javastrava.util.Paging)
	 */
	@Override
	public CompletableFuture<List<StravaActivity>> listRelatedActivitiesAsync(final Long id, final Paging pagingInstruction) {
		return StravaServiceImpl.future(() -> {
			return listRelatedActivities(id, pagingInstruction);
		});
	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#updateActivity(Integer,javastrava.api.v3.model.StravaActivityUpdate)
	 */
	@Override
	public StravaActivity updateActivity(final Long activityId, final StravaActivityUpdate activity)
			throws NotFoundException {
		final StravaActivityUpdate update = activity;
		if (activity == null) {
			return getActivity(activityId);
		}
		StravaActivity response = null;

		// Activity must exist to be updated
		final StravaActivity stravaActivity = getActivity(activityId);
		if (stravaActivity == null) {
			throw new NotFoundException(Messages.string("ActivityServiceImpl.updateInvalidActivity")); //$NON-NLS-1$
		}

		// Activity must not be private and inaccessible
		if (stravaActivity.getResourceState() == StravaResourceState.PRIVATE) {
			throw new UnauthorizedException(Messages.string("ActivityServiceImpl.updatePrivateActivity")); //$NON-NLS-1$
		}

		// TODO Workaround for issue javastrava-api #36
		// (https://github.com/danshannon/javastravav3api/issues/36)
		if (update.getCommute() != null) {
			final StravaActivityUpdate commuteUpdate = new StravaActivityUpdate();
			commuteUpdate.setCommute(update.getCommute());
			response = doUpdateActivity(activityId, commuteUpdate);
			if (response.getCommute() != update.getCommute()) {
				throw new StravaUnknownAPIException(
						Messages.string("ActivityServiceImpl.failedToUpdateCommuteFlag") + activityId, null, null); //$NON-NLS-1$
			}

			update.setCommute(null);
		}

		// End of workaround

		// Perform the update on Strava
		response = doUpdateActivity(activityId, update);

		// Put it back in the cache, unless it's UPDATING
		if (response.getResourceState() != StravaResourceState.UPDATING) {
			this.activityCache.put(response);
		}

		// Return the updated activity
		return response;

	}

	/**
	 * @see javastrava.api.v3.service.ActivityService#updateActivityAsync(java.lang.Integer, javastrava.api.v3.model.StravaActivityUpdate)
	 */
	@Override
	public CompletableFuture<StravaActivity> updateActivityAsync(final Long activityId, final StravaActivityUpdate activity) throws NotFoundException {
		return StravaServiceImpl.future(() -> {
			return updateActivity(activityId, activity);
		});
	}

}

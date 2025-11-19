public class ServiceContainer {

  private RequestHandler requestHandler;

  public ServiceContainer() {
    DataStore dataStore = new DataStore();
    ResponseUtil responseUtil = new ResponseUtil();
    UserService userService = new UserService(dataStore);
    SessionService sessionService = new SessionService(dataStore);
    GuestbookService guestbookService = new GuestbookService(dataStore);
    requestHandler = new RequestHandler(dataStore, userService, sessionService, guestbookService, responseUtil);
  }

  public RequestHandler getRequestHandler() {
    return requestHandler;
  }
}

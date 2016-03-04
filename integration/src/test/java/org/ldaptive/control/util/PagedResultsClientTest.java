/* See LICENSE for licensing and NOTICE for copyright. */
package org.ldaptive.control.util;

import java.util.Iterator;
import org.ldaptive.AbstractTest;
import org.ldaptive.Connection;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SortBehavior;
import org.ldaptive.TestUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit test for {@link PagedResultsClient}.
 *
 * @author  Middleware Services
 */
public class PagedResultsClientTest extends AbstractTest
{

  /** Entries created for ldap tests. */
  private static LdapEntry[] testLdapEntries;


  /**
   * @param  ldifFile1  to create.
   * @param  ldifFile2  to create.
   * @param  ldifFile3  to create.
   *
   * @throws  Exception  On test failure.
   */
  @Parameters(
    {
      "createEntry22",
      "createEntry23",
      "createEntry25"
    })
  @BeforeClass(groups = {"control-util"})
  public void createLdapEntry(final String ldifFile1, final String ldifFile2, final String ldifFile3)
    throws Exception
  {
    testLdapEntries = new LdapEntry[3];
    testLdapEntries[0] = TestUtils.convertLdifToResult(TestUtils.readFileIntoString(ldifFile1)).getEntry();
    super.createLdapEntry(testLdapEntries[0]);
    testLdapEntries[1] = TestUtils.convertLdifToResult(TestUtils.readFileIntoString(ldifFile2)).getEntry();
    super.createLdapEntry(testLdapEntries[1]);
    testLdapEntries[2] = TestUtils.convertLdifToResult(TestUtils.readFileIntoString(ldifFile3)).getEntry();
    super.createLdapEntry(testLdapEntries[2]);
  }


  /** @throws  Exception  On test failure. */
  @AfterClass(groups = {"control-util"})
  public void deleteLdapEntry()
    throws Exception
  {
    for (LdapEntry testLdapEntry : testLdapEntries) {
      super.deleteLdapEntry(testLdapEntry.getDn());
    }
  }


  /**
   * @param  dn  to search on.
   * @param  filter  to search with.
   *
   * @throws  Exception  On test failure.
   */
  @Parameters(
    {
      "prSearchDn",
      "prSearchFilter"
    })
  @Test(groups = {"control-util"})
  public void execute(final String dn, final String filter)
    throws Exception
  {
    try (Connection conn = TestUtils.createConnection()) {
      conn.open();

      final PagedResultsClient client = new PagedResultsClient(conn, 1);

      final SearchRequest request = new SearchRequest(dn, new SearchFilter(filter));
      Response<SearchResult> response = client.execute(request);
      AssertJUnit.assertEquals(ResultCode.SUCCESS, response.getResultCode());
      AssertJUnit.assertEquals(1, response.getResult().size());
      AssertJUnit.assertEquals(
        testLdapEntries[0].getDn().toLowerCase(),
        response.getResult().getEntry().getDn().toLowerCase());

      int i = 1;
      while (client.hasMore(response)) {
        response = client.execute(request, response);
        AssertJUnit.assertEquals(ResultCode.SUCCESS, response.getResultCode());
        AssertJUnit.assertEquals(1, response.getResult().size());
        AssertJUnit.assertEquals(
          testLdapEntries[i].getDn().toLowerCase(),
          response.getResult().getEntry().getDn().toLowerCase());
        i++;
      }

      try {
        client.execute(request, response);
      } catch (IllegalArgumentException e) {
        AssertJUnit.assertNotNull(e);
      }
    } catch (LdapException e) {
      // ignore this test if not supported by the server
      AssertJUnit.assertEquals(ResultCode.UNAVAILABLE_CRITICAL_EXTENSION, e.getResultCode());
    }
  }


  /**
   * @param  dn  to search on.
   * @param  filter  to search with.
   *
   * @throws  Exception  On test failure.
   */
  @Parameters(
    {
      "prSearchDn",
      "prSearchFilter"
    })
  @Test(groups = {"control-util"})
  public void executeToCompletion(final String dn, final String filter)
    throws Exception
  {
    try (Connection conn = TestUtils.createConnection()) {
      conn.open();

      final PagedResultsClient client = new PagedResultsClient(conn, 1);

      final SearchRequest request = new SearchRequest(dn, new SearchFilter(filter));
      request.setSortBehavior(SortBehavior.SORTED);

      final Response<SearchResult> response = client.executeToCompletion(request);
      AssertJUnit.assertEquals(ResultCode.SUCCESS, response.getResultCode());
      AssertJUnit.assertEquals(3, response.getResult().size());

      final Iterator<LdapEntry> i = response.getResult().getEntries().iterator();
      AssertJUnit.assertEquals(ResultCode.SUCCESS, response.getResultCode());
      AssertJUnit.assertEquals(testLdapEntries[1].getDn().toLowerCase(), i.next().getDn().toLowerCase());
      AssertJUnit.assertEquals(testLdapEntries[0].getDn().toLowerCase(), i.next().getDn().toLowerCase());
      AssertJUnit.assertEquals(testLdapEntries[2].getDn().toLowerCase(), i.next().getDn().toLowerCase());
    } catch (LdapException e) {
      // ignore this test if not supported by the server
      AssertJUnit.assertEquals(ResultCode.UNAVAILABLE_CRITICAL_EXTENSION, e.getResultCode());
    }
  }
}

import 'package:flutter/src/foundation/key.dart';
import 'package:flutter/src/widgets/framework.dart';
import 'package:flutter/material.dart';

import 'package:url_launcher/url_launcher.dart';

import 'package:html/parser.dart';

import 'package:http/http.dart' as http;

class Result extends StatelessWidget {
  Result({Key? key, required String URL, required String searchedWord})
      : _URL = URL,
        _searchedWord = searchedWord,
        super(key: key);

  String _URL, _searchedWord;

  @override
  Widget build(BuildContext context) {
    var url = Uri.parse(_URL);
    var response = http.get(url);
    return FutureBuilder(
      future: response,
      builder: (BuildContext context, AsyncSnapshot<http.Response> snapshot) {
        if (snapshot.hasData) {
          var document = parse(snapshot.data!.body);
          var title = document.querySelector('title');
          var body = document.querySelector('body');

          // regular expression:
          RegExp re = new RegExp(r"(\w|\s|,|')+[。.?!]*\s*");

          // get all the matches:
          Iterable matches = re.allMatches(body!.text);

          // loop over the matches:
          List<String> matchesList = [];
          for (var match in matches) {
            matchesList.add(match.group(0));
          }
          // loop over the matches:
          String des = '';
          for (var i = 0; i < matchesList.length; i++) {
            if (matchesList[i].contains(_searchedWord)) {
              des = matchesList[i];
              break;
            }
          }

          return InkWell(
            onTap: () {
              _launchUrl(Uri.parse(_URL));
            },
            child: Card(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(style: TextStyle(color: Colors.blue), title!.text),
                  //URl
                  Text(style: TextStyle(color: Colors.green), _URL),
                  //Description
                  Text(des),
                  //Text(matches.length.toString()),
                ],
              ),
            ),
          );
        } else {
          return const Text('Loading...');
        }
      },
    );
  }

  void _launchUrl(Uri url) async {
    if (!await launchUrl(url)) throw 'Could not launch $url';
  }
}
